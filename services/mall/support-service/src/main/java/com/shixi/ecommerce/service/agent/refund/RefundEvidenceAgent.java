package com.shixi.ecommerce.service.agent.refund;

import org.springframework.stereotype.Service;

@Service
public class RefundEvidenceAgent implements RefundSubAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;

    public RefundEvidenceAgent(AgentProfileRegistry profileRegistry, RagService ragService, ModelClient modelClient) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
    }

    @Override
    public String getType() {
        return RefundAgentTypes.EVIDENCE;
    }

    @Override
    public RefundAgentOutput handle(RefundContext context) {
        String reason = context.getSlot(RefundSlots.REASON);
        boolean needsEvidence = RefundReasonType.QUALITY.name().equals(reason)
                || RefundReasonType.WRONG_ITEM.name().equals(reason)
                || RefundReasonType.MISSING_PARTS.name().equals(reason);
        boolean hasEvidence = context.hasSlot(RefundSlots.EVIDENCE);
        AgentProfile profile = profileRegistry.getProfile(getType());

        String prompt;
        if (!needsEvidence) {
            prompt = "Evidence not required for this scenario.";
        } else if (!hasEvidence) {
            prompt = "Request photo/video evidence for quality or wrong-item refund.";
        } else {
            prompt = "Evidence received. Proceed to policy review.";
        }
        var docs = ragService.retrieve(prompt, profile.getRagCollection());
        String text = modelClient.generate(profile, prompt, docs);
        return new RefundAgentOutput(text, null, null, String.join(" | ", docs));
    }
}
