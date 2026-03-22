package com.shixi.ecommerce.service.agent.refund;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RefundOrderAgent implements RefundSubAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;

    public RefundOrderAgent(AgentProfileRegistry profileRegistry, RagService ragService, ModelClient modelClient) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
    }

    @Override
    public String getType() {
        return RefundAgentTypes.ORDER;
    }

    @Override
    public RefundAgentOutput handle(RefundContext context) {
        String orderNo = context.getSlot(RefundSlots.ORDER_NO);
        AgentProfile profile = profileRegistry.getProfile(getType());
        String prompt;
        if (orderNo == null) {
            prompt = "Ask customer to provide order number before refund handling.";
        } else {
            prompt = "Order number received: " + orderNo + ". Confirm and proceed to reason check.";
        }
        var docs = ragService.retrieve(prompt, profile.getRagCollection());
        String text = modelClient.generate(profile, prompt, docs);
        return new RefundAgentOutput(text, null, null, String.join(" | ", docs));
    }
}
