package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundDecision;

import java.util.Collections;
import java.util.Map;

public class RefundSkillOutput {
    private final String prompt;
    private final Map<String, String> updates;
    private final RefundDecision decision;

    public RefundSkillOutput(String prompt) {
        this(prompt, Collections.emptyMap(), null);
    }

    public RefundSkillOutput(String prompt, Map<String, String> updates, RefundDecision decision) {
        this.prompt = prompt;
        this.updates = updates == null ? Collections.emptyMap() : updates;
        this.decision = decision;
    }

    public String getPrompt() {
        return prompt;
    }

    public Map<String, String> getUpdates() {
        return updates;
    }

    public RefundDecision getDecision() {
        return decision;
    }
}
