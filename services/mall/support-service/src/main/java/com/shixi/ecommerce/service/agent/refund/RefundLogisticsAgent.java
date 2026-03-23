package com.shixi.ecommerce.service.agent.refund;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RefundLogisticsAgent implements RefundSubAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;

    public RefundLogisticsAgent(AgentProfileRegistry profileRegistry, RagService ragService, ModelClient modelClient) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
    }

    @Override
    public String getType() {
        return RefundAgentTypes.LOGISTICS;
    }

    @Override
    public RefundAgentOutput handle(RefundContext context) {
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

        AgentProfile profile = profileRegistry.getProfile(getType());
        String prompt = "Logistics action: " + action;
        var docs = ragService.retrieve(prompt, profile.getRagCollection());
        String text = modelClient.generate(profile, prompt, docs);
        return new RefundAgentOutput(text, updates, null, String.join(" | ", docs));
    }
}
