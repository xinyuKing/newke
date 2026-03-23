package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.IntentType;
import com.shixi.ecommerce.domain.SessionState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefundContext {
    private static final int MAX_FEEDBACK_HINTS = 8;

    private String sessionId;
    private String message;
    private IntentType intent;
    private SessionState state;
    private RefundDecision decision;
    private final Map<String, String> slots = new HashMap<>();
    private final List<String> feedbackHints = new ArrayList<>();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public IntentType getIntent() {
        return intent;
    }

    public void setIntent(IntentType intent) {
        this.intent = intent;
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public RefundDecision getDecision() {
        return decision;
    }

    public void setDecision(RefundDecision decision) {
        this.decision = decision;
    }

    public Map<String, String> getSlots() {
        return slots;
    }

    public String getSlot(String key) {
        return slots.get(key);
    }

    public void putSlot(String key, String value) {
        if (value != null && !value.isBlank()) {
            slots.put(key, value);
        }
    }

    public boolean hasSlot(String key) {
        String value = slots.get(key);
        return value != null && !value.isBlank();
    }

    public List<String> getFeedbackHints() {
        return feedbackHints;
    }

    public void addFeedbackHint(String hint) {
        if (hint != null && !hint.isBlank()) {
            if (feedbackHints.size() >= MAX_FEEDBACK_HINTS) {
                feedbackHints.remove(0);
            }
            feedbackHints.add(hint);
        }
    }

    public void replaceFeedbackHints(List<String> hints) {
        feedbackHints.clear();
        if (hints == null) {
            return;
        }
        for (String hint : hints) {
            addFeedbackHint(hint);
        }
    }
}
