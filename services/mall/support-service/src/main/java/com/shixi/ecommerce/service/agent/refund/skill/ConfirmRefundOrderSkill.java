package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import org.springframework.stereotype.Service;

@Service
public class ConfirmRefundOrderSkill extends AbstractRefundSkill<RefundSkillOutput> {
    @Override
    public String getName() {
        return RefundSkillNames.CONFIRM_ORDER;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        String orderNo = context.getSlot(RefundSlots.ORDER_NO);
        String prompt;
        if (orderNo == null) {
            prompt = "Ask customer to provide order number before refund handling.";
        } else {
            prompt = "Order number received: " + orderNo + ". Confirm and proceed to reason check.";
        }
        return new RefundSkillOutput(prompt);
    }
}
