package com.shixi.ecommerce.service.agent;

import com.shixi.ecommerce.domain.IntentType;
import com.shixi.ecommerce.service.agent.refund.RefundIntentClassifier;
import org.springframework.stereotype.Component;

@Component
public class IntentRecognizer {
    private final RefundIntentClassifier refundIntentClassifier;

    public IntentRecognizer(RefundIntentClassifier refundIntentClassifier) {
        this.refundIntentClassifier = refundIntentClassifier;
    }

    public IntentType recognize(String message) {
        String text = message == null ? "" : message;
        if (refundIntentClassifier.isRefund(text)) {
            return IntentType.REFUND;
        }
        String lower = text.toLowerCase();
        boolean logistics = containsAny(
                lower,
                "\u7269\u6d41",
                "\u5feb\u9012",
                "\u914d\u9001",
                "\u6d3e\u9001",
                "\u6536\u8d27",
                "\u8fd0\u5355",
                "logistics",
                "delivery",
                "shipment",
                "tracking");
        boolean consult = containsAny(
                lower,
                "\u54a8\u8be2",
                "\u4ef7\u683c",
                "\u4f18\u60e0",
                "\u53d1\u8d27",
                "\u5230\u8d27",
                "\u5e93\u5b58",
                "price",
                "discount",
                "stock",
                "sku");
        if (logistics) {
            return IntentType.LOGISTICS;
        }
        if (consult) {
            return IntentType.CONSULT;
        }
        return IntentType.UNKNOWN;
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
