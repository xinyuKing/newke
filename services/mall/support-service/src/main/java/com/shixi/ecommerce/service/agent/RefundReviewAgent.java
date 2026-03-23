package com.shixi.ecommerce.service.agent;

import org.springframework.stereotype.Component;

@Component
public class RefundReviewAgent implements Agent {
    @Override
    public String getType() {
        return "REFUND_REVIEW";
    }

    @Override
    public String handle(String sessionId, String message) {
        String text = message == null ? "" : message;
        if (text.contains("\u7834\u635f") || text.contains("\u8d28\u91cf") || text.contains("\u7f3a\u4ef6")) {
            return "Refund review: potential quality issue. Please provide photo/video evidence.";
        }
        if (text.contains("\u4e0d\u559c\u6b22") || text.contains("\u4e0d\u5408\u9002")) {
            return "Refund review: possible no-reason return. Confirm within policy window and item condition.";
        }
        return "Refund review: more evidence needed (photos, delivery timestamps).";
    }
}
