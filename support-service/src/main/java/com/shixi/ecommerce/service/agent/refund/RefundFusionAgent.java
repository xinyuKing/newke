package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.SessionState;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RefundFusionAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;

    public RefundFusionAgent(AgentProfileRegistry profileRegistry, RagService ragService, ModelClient modelClient) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
    }

    public FusionResult fuse(RefundContext context, RefundTaskPlan plan, List<RefundAgentOutput> outputs) {
        SessionState state = plan.getNextState();
        String orderNo = context.getSlot(RefundSlots.ORDER_NO);
        String reason = context.getSlot(RefundSlots.REASON);
        String requestType = context.getSlot(RefundSlots.REQUEST_TYPE);
        String policyResult = context.getSlot(RefundSlots.POLICY_RESULT);
        String logisticsAction = context.getSlot(RefundSlots.LOGISTICS_ACTION);

        StringBuilder reply = new StringBuilder();
        reply.append("Refund assistant: ");
        if (state == SessionState.WAIT_ORDER) {
            reply.append("please provide the order number to start refund verification.");
        } else if (state == SessionState.WAIT_REASON) {
            reply.append("order received. Please describe the refund reason and item condition.");
        } else if (state == SessionState.WAIT_EVIDENCE) {
            reply.append("please upload photo/video evidence for the issue.");
        } else {
            reply.append("order=").append(orderNo == null ? "unknown" : orderNo);
            if (reason != null) {
                reply.append(", reason=").append(reason);
            }
            if (requestType != null) {
                reply.append(", requestType=").append(requestType);
            }
            reply.append(". ");

            if (policyResult != null) {
                reply.append("Policy: ").append(policyResult).append(" ");
            }
            if (logisticsAction != null) {
                reply.append("Logistics: ").append(logisticsAction).append(" ");
            }

            if (context.getDecision() == RefundDecision.APPROVE) {
                reply.append("Eligible for refund. Please confirm pickup address and bank/account details.");
            } else if (context.getDecision() == RefundDecision.NEED_INFO) {
                reply.append("Need more info: purchase date, item condition, and whether accessories are intact.");
            } else if (context.getDecision() == RefundDecision.MANUAL_REVIEW) {
                reply.append("Submitted for manual review. Expect update within 1-2 business days.");
            } else if (context.getDecision() == RefundDecision.REJECT) {
                reply.append("Not eligible under current policy. You may request manual review.");
            } else {
                reply.append("Next step: confirm details so we can proceed.");
            }

            if (RefundReasonType.DELAYED.name().equals(reason)) {
                reply.append(" Options: keep waiting for delivery or accept partial refund/compensation.");
            } else if (RefundReasonType.NOT_RECEIVED.name().equals(reason)) {
                reply.append(" Please confirm delivery address and contact phone for carrier verification.");
            } else if (RefundReasonType.NO_REASON.name().equals(reason)) {
                reply.append(" If within 7 days and unused, return is supported.");
            } else if (RefundReasonType.SIZE_ISSUE.name().equals(reason)) {
                reply.append(" Exchange is recommended if size/fit is the only issue.");
            }

            if (RefundRequestType.EXCHANGE.name().equals(requestType)) {
                reply.append(" Exchange flow: confirm target size/color before pickup.");
            } else if (RefundRequestType.RETURN_REFUND.name().equals(requestType)) {
                reply.append(" Return flow: keep original packaging if possible.");
            }
        }

        if (!context.getFeedbackHints().isEmpty()) {
            reply.append(" Feedback: ").append(String.join("; ", context.getFeedbackHints()));
        }

        AgentProfile profile = profileRegistry.getProfile(RefundAgentTypes.FUSION);
        String prompt = reply.toString();
        List<String> docs = ragService.retrieve(prompt, profile.getRagCollection());
        String result = modelClient.generate(profile, prompt, docs);
        return new FusionResult(result, String.join(" | ", docs));
    }
}
