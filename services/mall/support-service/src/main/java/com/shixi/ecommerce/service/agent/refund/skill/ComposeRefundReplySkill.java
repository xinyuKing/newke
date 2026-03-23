package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundDecision;
import com.shixi.ecommerce.service.agent.refund.RefundReasonType;
import com.shixi.ecommerce.service.agent.refund.RefundRequestType;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import com.shixi.ecommerce.service.agent.refund.RefundTaskPlan;
import org.springframework.stereotype.Service;

@Service
public class ComposeRefundReplySkill extends AbstractRefundSkill<RefundSkillOutput> {
    @Override
    public String getName() {
        return RefundSkillNames.COMPOSE_REPLY;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        RefundTaskPlan plan = requirePlan(request);
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
            appendReviewReply(reply, context, orderNo, reason, requestType, policyResult, logisticsAction);
        }

        if (!context.getFeedbackHints().isEmpty()) {
            reply.append(" Feedback: ").append(String.join("; ", context.getFeedbackHints()));
        }
        return new RefundSkillOutput(reply.toString());
    }

    private void appendReviewReply(
            StringBuilder reply,
            RefundContext context,
            String orderNo,
            String reason,
            String requestType,
            String policyResult,
            String logisticsAction) {
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

        appendDecisionReply(reply, context.getDecision());
        appendReasonReply(reply, reason);
        appendRequestTypeReply(reply, requestType);
    }

    private void appendDecisionReply(StringBuilder reply, RefundDecision decision) {
        if (decision == RefundDecision.APPROVE) {
            reply.append("Eligible for refund. Please confirm pickup address and bank/account details.");
        } else if (decision == RefundDecision.NEED_INFO) {
            reply.append("Need more info: purchase date, item condition, and whether accessories are intact.");
        } else if (decision == RefundDecision.MANUAL_REVIEW) {
            reply.append("Submitted for manual review. Expect update within 1-2 business days.");
        } else if (decision == RefundDecision.REJECT) {
            reply.append("Not eligible under current policy. You may request manual review.");
        } else {
            reply.append("Next step: confirm details so we can proceed.");
        }
    }

    private void appendReasonReply(StringBuilder reply, String reason) {
        if (RefundReasonType.DELAYED.name().equals(reason)) {
            reply.append(" Options: keep waiting for delivery or accept partial refund/compensation.");
        } else if (RefundReasonType.NOT_RECEIVED.name().equals(reason)) {
            reply.append(" Please confirm delivery address and contact phone for carrier verification.");
        } else if (RefundReasonType.NO_REASON.name().equals(reason)) {
            reply.append(" If within 7 days and unused, return is supported.");
        } else if (RefundReasonType.SIZE_ISSUE.name().equals(reason)) {
            reply.append(" Exchange is recommended if size/fit is the only issue.");
        }
    }

    private void appendRequestTypeReply(StringBuilder reply, String requestType) {
        if (RefundRequestType.EXCHANGE.name().equals(requestType)) {
            reply.append(" Exchange flow: confirm target size/color before pickup.");
        } else if (RefundRequestType.RETURN_REFUND.name().equals(requestType)) {
            reply.append(" Return flow: keep original packaging if possible.");
        }
    }
}
