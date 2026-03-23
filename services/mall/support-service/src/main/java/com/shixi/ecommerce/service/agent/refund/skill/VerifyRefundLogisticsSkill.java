package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundDeliveryStatus;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class VerifyRefundLogisticsSkill extends AbstractRefundSkill<RefundSkillOutput> {
    @Override
    public String getName() {
        return RefundSkillNames.VERIFY_LOGISTICS;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        String deliveryStatus = context.getSlot(RefundSlots.DELIVERY_STATUS);
        String action;
        if (RefundDeliveryStatus.NOT_RECEIVED.name().equals(deliveryStatus)) {
            action = "Verify carrier tracking and confirm delivery address.";
        } else if (RefundDeliveryStatus.DELIVERED.name().equals(deliveryStatus)) {
            action = "Request return shipment tracking after pickup confirmation.";
        } else {
            action = "Ask whether item has been delivered.";
        }

        Map<String, String> updates = new HashMap<>();
        updates.put(RefundSlots.LOGISTICS_ACTION, action);
        String prompt = "Logistics action: " + action;
        return new RefundSkillOutput(prompt, updates, null);
    }
}
