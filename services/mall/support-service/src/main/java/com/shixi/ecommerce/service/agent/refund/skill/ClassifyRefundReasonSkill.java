package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import org.springframework.stereotype.Service;

@Service
public class ClassifyRefundReasonSkill extends AbstractRefundSkill<RefundSkillOutput> {
    @Override
    public String getName() {
        return RefundSkillNames.CLASSIFY_REASON;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        String reason = context.getSlot(RefundSlots.REASON);
        String prompt;
        if (reason == null) {
            prompt = "Ask customer for refund reason and scenario details.";
        } else {
            prompt = "Refund reason classified as " + reason + ".";
        }
        return new RefundSkillOutput(prompt);
    }
}
