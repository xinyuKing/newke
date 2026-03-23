package com.shixi.ecommerce.service.agent;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LogisticsAgent implements Agent {
    @Override
    public String getType() {
        return "LOGISTICS";
    }

    @Override
    public String handle(String sessionId, String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (containsAny(text, "未收到", "没收到", "not received")) {
            return "Logistics: package not received. Please provide the order number or tracking number. "
                    + "If tracking has not updated for 48 hours, we will escalate for manual verification.";
        }
        if (containsAny(text, "签收", "已收货", "delivered", "received")) {
            return "Logistics: if the parcel shows delivered but you have not received it, "
                    + "please confirm the delivery address and contact phone so we can verify proof of delivery.";
        }
        return "Logistics: tracking requested. Please provide the order number or tracking number. "
                + "If there is no update within 48 hours, manual intervention is recommended.";
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
