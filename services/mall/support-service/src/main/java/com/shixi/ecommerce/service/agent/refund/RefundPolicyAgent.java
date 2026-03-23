package com.shixi.ecommerce.service.agent.refund;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RefundPolicyAgent implements RefundSubAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;

    public RefundPolicyAgent(AgentProfileRegistry profileRegistry, RagService ragService, ModelClient modelClient) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
    }

    @Override
    public String getType() {
        return RefundAgentTypes.POLICY;
    }

    @Override
    public RefundAgentOutput handle(RefundContext context) {
        String reason = context.getSlot(RefundSlots.REASON);
        boolean hasEvidence = context.hasSlot(RefundSlots.EVIDENCE);

        RefundDecision decision;
        String policyResult;

        if (RefundReasonType.NOT_RECEIVED.name().equals(reason)) {
            decision = RefundDecision.MANUAL_REVIEW;
            policyResult = "Not-received claims require carrier verification.";
        } else if (RefundReasonType.QUALITY.name().equals(reason)
                || RefundReasonType.WRONG_ITEM.name().equals(reason)
                || RefundReasonType.MISSING_PARTS.name().equals(reason)) {
            if (hasEvidence) {
                decision = RefundDecision.APPROVE;
                policyResult = "Evidence provided. Eligible for refund/return.";
            } else {
                decision = RefundDecision.NEED_INFO;
                policyResult = "Evidence required for quality or wrong-item cases.";
            }
        } else if (RefundReasonType.NO_REASON.name().equals(reason)
                || RefundReasonType.CHANGE_MIND.name().equals(reason)
                || RefundReasonType.SIZE_ISSUE.name().equals(reason)) {
            decision = RefundDecision.NEED_INFO;
            policyResult = "Check policy window and item condition for no-reason return.";
        } else if (RefundReasonType.DELAYED.name().equals(reason)) {
            decision = RefundDecision.MANUAL_REVIEW;
            policyResult = "Delay compensation or partial refund may apply after verification.";
        } else {
            decision = RefundDecision.MANUAL_REVIEW;
            policyResult = "Scenario requires manual policy review.";
        }

        Map<String, String> updates = new HashMap<>();
        updates.put(RefundSlots.POLICY_RESULT, policyResult);

        AgentProfile profile = profileRegistry.getProfile(getType());
        String prompt = "Policy result: " + policyResult + " decision=" + decision;
        var docs = ragService.retrieve(prompt, profile.getRagCollection());
        String text = modelClient.generate(profile, prompt, docs);
        return new RefundAgentOutput(text, updates, decision, String.join(" | ", docs));
    }
}
