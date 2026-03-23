package com.shixi.ecommerce.service.agent.refund;

import org.springframework.stereotype.Service;

@Service
public class RefundReasonAgent implements RefundSubAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;

    public RefundReasonAgent(AgentProfileRegistry profileRegistry, RagService ragService, ModelClient modelClient) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
    }

    @Override
    public String getType() {
        return RefundAgentTypes.REASON;
    }

    @Override
    public RefundAgentOutput handle(RefundContext context) {
        String reason = context.getSlot(RefundSlots.REASON);
        AgentProfile profile = profileRegistry.getProfile(getType());
        String prompt;
        if (reason == null) {
            prompt = "Ask customer for refund reason and scenario details.";
        } else {
            prompt = "Refund reason classified as " + reason + ".";
        }
        var docs = ragService.retrieve(prompt, profile.getRagCollection());
        String text = modelClient.generate(profile, prompt, docs);
        return new RefundAgentOutput(text, null, null, String.join(" | ", docs));
    }
}
