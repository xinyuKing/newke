package com.shixi.ecommerce.service.agent.refund;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class RefundRiskAgent implements RefundSubAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;

    public RefundRiskAgent(AgentProfileRegistry profileRegistry, RagService ragService, ModelClient modelClient) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
    }

    @Override
    public String getType() {
        return RefundAgentTypes.RISK;
    }

    @Override
    public RefundAgentOutput handle(RefundContext context) {
        String text = context.getMessage() == null ? "" : context.getMessage().toLowerCase(Locale.ROOT);
        String riskLevel = "LOW";
        if (containsAny(text, "\u591a\u6b21", "\u9891\u7e41", "frequent", "many times")) {
            riskLevel = "MEDIUM";
        }
        if (containsAny(text, "\u6b3a\u8bc8", "\u7ea0\u7eb7", "chargeback", "fraud")) {
            riskLevel = "HIGH";
        }
        Map<String, String> updates = new HashMap<>();
        updates.put(RefundSlots.RISK_LEVEL, riskLevel);

        AgentProfile profile = profileRegistry.getProfile(getType());
        String prompt = "Risk level=" + riskLevel + ". Apply manual review if high value.";
        var docs = ragService.retrieve(prompt, profile.getRagCollection());
        String reply = modelClient.generate(profile, prompt, docs);
        RefundDecision decision = "HIGH".equals(riskLevel) || "MEDIUM".equals(riskLevel)
                ? RefundDecision.MANUAL_REVIEW
                : null;
        return new RefundAgentOutput(reply, updates, decision, String.join(" | ", docs));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
