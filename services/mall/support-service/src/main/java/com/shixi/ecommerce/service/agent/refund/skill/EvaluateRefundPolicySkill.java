package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundDecision;
import com.shixi.ecommerce.service.agent.refund.RefundReasonType;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class EvaluateRefundPolicySkill extends AbstractRefundSkill<RefundSkillOutput> {
    @Override
    public String getName() {
        return RefundSkillNames.EVALUATE_POLICY;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
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
        String prompt = "Policy result: " + policyResult + " decision=" + decision;
        return new RefundSkillOutput(prompt, updates, decision);
    }
}
