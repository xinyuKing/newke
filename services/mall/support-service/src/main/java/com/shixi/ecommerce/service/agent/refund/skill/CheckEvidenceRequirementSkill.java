package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundReasonType;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import org.springframework.stereotype.Service;

@Service
public class CheckEvidenceRequirementSkill extends AbstractRefundSkill<RefundSkillOutput> {
    @Override
    public String getName() {
        return RefundSkillNames.CHECK_EVIDENCE;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        String reason = context.getSlot(RefundSlots.REASON);
        boolean needsEvidence = RefundReasonType.QUALITY.name().equals(reason)
                || RefundReasonType.WRONG_ITEM.name().equals(reason)
                || RefundReasonType.MISSING_PARTS.name().equals(reason);
        boolean hasEvidence = context.hasSlot(RefundSlots.EVIDENCE);

        String prompt;
        if (!needsEvidence) {
            prompt = "Evidence not required for this scenario.";
        } else if (!hasEvidence) {
            prompt = "Request photo/video evidence for quality or wrong-item refund.";
        } else {
            prompt = "Evidence received. Proceed to policy review.";
        }
        return new RefundSkillOutput(prompt);
    }
}
