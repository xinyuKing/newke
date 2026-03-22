package com.shixi.ecommerce.service.agent.refund;

import java.util.Collections;
import java.util.Map;

public class RefundAgentOutput {
    private final String message;
    private final Map<String, String> updates;
    private final RefundDecision decision;
    private final String meta;

    public RefundAgentOutput(String message) {
        this(message, Collections.emptyMap(), null, null);
    }

    public RefundAgentOutput(String message, Map<String, String> updates, RefundDecision decision) {
        this(message, updates, decision, null);
    }

    public RefundAgentOutput(String message, Map<String, String> updates, RefundDecision decision, String meta) {
        this.message = message;
        this.updates = updates == null ? Collections.emptyMap() : updates;
        this.decision = decision;
        this.meta = meta;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getUpdates() {
        return updates;
    }

    public RefundDecision getDecision() {
        return decision;
    }

    public String getMeta() {
        return meta;
    }
}
