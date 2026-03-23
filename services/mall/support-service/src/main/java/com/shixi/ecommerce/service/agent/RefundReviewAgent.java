package com.shixi.ecommerce.service.agent;

import java.util.Locale;
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
        String lower = text.toLowerCase(Locale.ROOT);
        if (!containsAny(lower, "order", "订单", "单号") && !lower.matches(".*\\b\\d{8,24}\\b.*")) {
            return "Refund review: please provide the order number first so we can verify the refund case.";
        }
        if (text.contains("\u7834\u635f") || text.contains("\u8d28\u91cf") || text.contains("\u7f3a\u4ef6")) {
            return "Refund review: potential quality issue. Please provide photo or video evidence "
                    + "and keep the original packaging if possible.";
        }
        if (text.contains("\u4e0d\u559c\u6b22") || text.contains("\u4e0d\u5408\u9002")) {
            return "Refund review: possible no-reason return. Please confirm whether the request "
                    + "is within the policy window and whether the item is unused.";
        }
        return "Refund review: more evidence needed (photos, delivery timestamps).";
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
