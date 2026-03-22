package com.shixi.ecommerce.service.agent.refund;

public class FeedbackResult {
    private final boolean ok;
    private final String reason;
    private final String meta;

    public FeedbackResult(boolean ok, String reason) {
        this(ok, reason, null);
    }

    public FeedbackResult(boolean ok, String reason, String meta) {
        this.ok = ok;
        this.reason = reason;
        this.meta = meta;
    }

    public boolean isOk() {
        return ok;
    }

    public String getReason() {
        return reason;
    }

    public String getMeta() {
        return meta;
    }
}
