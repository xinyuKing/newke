package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundMessageParser;
import com.shixi.ecommerce.service.agent.refund.RefundReasonType;
import com.shixi.ecommerce.service.agent.refund.RefundRequestType;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import org.springframework.stereotype.Service;

@Service
public class ExtractRefundSlotsSkill extends AbstractRefundSkill<RefundContext> {
    private final RefundMessageParser parser;

    public ExtractRefundSlotsSkill(RefundMessageParser parser) {
        this.parser = parser;
    }

    @Override
    public String getName() {
        return RefundSkillNames.EXTRACT_REFUND_SLOTS;
    }

    @Override
    public RefundContext execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        parser.enrich(context);
        ensureDefaultRequestType(context);
        return context;
    }

    private void ensureDefaultRequestType(RefundContext context) {
        if (context.hasSlot(RefundSlots.REQUEST_TYPE)) {
            return;
        }
        String reason = context.getSlot(RefundSlots.REASON);
        if (RefundReasonType.NOT_RECEIVED.name().equals(reason)) {
            context.putSlot(RefundSlots.REQUEST_TYPE, RefundRequestType.REFUND_ONLY.name());
        } else if (reason != null) {
            context.putSlot(RefundSlots.REQUEST_TYPE, RefundRequestType.RETURN_REFUND.name());
        }
    }
}
