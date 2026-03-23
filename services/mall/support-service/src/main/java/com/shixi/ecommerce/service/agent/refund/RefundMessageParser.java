package com.shixi.ecommerce.service.agent.refund;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RefundMessageParser {
    private static final Pattern ORDER_PATTERN = Pattern.compile(
            "(?i)(?:order|ord|\\u8ba2\\u5355|\\u5355\\u53f7)[:#\\s]*([a-z0-9_-]{6,32})");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\b(\\d{8,24})\\b");

    public void enrich(RefundContext context) {
        String message = context.getMessage();
        if (message == null) {
            return;
        }
        extractOrderNo(context, message);
        extractRequestType(context, message);
        extractDeliveryStatus(context, message);
        extractReason(context, message);
        extractEvidence(context, message);
    }

    private void extractOrderNo(RefundContext context, String message) {
        if (context.hasSlot(RefundSlots.ORDER_NO)) {
            return;
        }
        Matcher matcher = ORDER_PATTERN.matcher(message);
        if (matcher.find()) {
            context.putSlot(RefundSlots.ORDER_NO, matcher.group(1));
            return;
        }
        Matcher digit = DIGIT_PATTERN.matcher(message);
        if (digit.find()) {
            context.putSlot(RefundSlots.ORDER_NO, digit.group(1));
        }
    }

    private void extractRequestType(RefundContext context, String message) {
        if (context.hasSlot(RefundSlots.REQUEST_TYPE)) {
            return;
        }
        String text = message.toLowerCase(Locale.ROOT);
        if (containsAny(text, "\u6362\u8d27", "exchange")) {
            context.putSlot(RefundSlots.REQUEST_TYPE, RefundRequestType.EXCHANGE.name());
        } else if (containsAny(text, "\u9000\u8d27", "return")) {
            context.putSlot(RefundSlots.REQUEST_TYPE, RefundRequestType.RETURN_REFUND.name());
        } else if (containsAny(text, "\u9000\u6b3e", "refund")) {
            context.putSlot(RefundSlots.REQUEST_TYPE, RefundRequestType.REFUND_ONLY.name());
        }
    }

    private void extractDeliveryStatus(RefundContext context, String message) {
        if (context.hasSlot(RefundSlots.DELIVERY_STATUS)) {
            return;
        }
        String text = message.toLowerCase(Locale.ROOT);
        if (containsAny(text, "\u672a\u6536\u5230", "\u6ca1\u6536\u5230", "not received")) {
            context.putSlot(RefundSlots.DELIVERY_STATUS, RefundDeliveryStatus.NOT_RECEIVED.name());
        } else if (containsAny(text, "\u5df2\u6536\u5230", "\u6536\u5230\u8d27", "delivered", "received")) {
            context.putSlot(RefundSlots.DELIVERY_STATUS, RefundDeliveryStatus.DELIVERED.name());
        }
    }

    private void extractReason(RefundContext context, String message) {
        if (context.hasSlot(RefundSlots.REASON)) {
            return;
        }
        String text = message.toLowerCase(Locale.ROOT);
        if (containsAny(text, "\u8d28\u91cf", "\u7834\u635f", "\u574f\u4e86", "broken", "defect")) {
            context.putSlot(RefundSlots.REASON, RefundReasonType.QUALITY.name());
        } else if (containsAny(text, "\u9519\u53d1", "\u53d1\u9519", "\u9519\u8d27", "wrong item")) {
            context.putSlot(RefundSlots.REASON, RefundReasonType.WRONG_ITEM.name());
        } else if (containsAny(text, "\u7f3a\u4ef6", "\u5c11\u4ef6", "missing parts", "missing")) {
            context.putSlot(RefundSlots.REASON, RefundReasonType.MISSING_PARTS.name());
        } else if (containsAny(text, "\u672a\u6536\u5230", "\u6ca1\u6536\u5230", "not received")) {
            context.putSlot(RefundSlots.REASON, RefundReasonType.NOT_RECEIVED.name());
        } else if (containsAny(text, "\u5ef6\u8bef", "\u8fdf\u5230", "\u665a\u5230", "late", "delay")) {
            context.putSlot(RefundSlots.REASON, RefundReasonType.DELAYED.name());
        } else if (containsAny(text, "\u4e03\u5929\u65e0\u7406\u7531", "\u65e0\u7406\u7531", "no reason")) {
            context.putSlot(RefundSlots.REASON, RefundReasonType.NO_REASON.name());
        } else if (containsAny(text, "\u4e0d\u60f3\u8981", "\u4e0d\u559c\u6b22", "change mind")) {
            context.putSlot(RefundSlots.REASON, RefundReasonType.CHANGE_MIND.name());
        } else if (containsAny(text, "\u4e0d\u5408\u9002", "\u5c3a\u7801", "size", "fit")) {
            context.putSlot(RefundSlots.REASON, RefundReasonType.SIZE_ISSUE.name());
        } else if (containsAny(text, "\u90e8\u5206", "\u53ea\u9000", "partial")) {
            context.putSlot(RefundSlots.REASON, RefundReasonType.OTHER.name());
        }
    }

    private void extractEvidence(RefundContext context, String message) {
        String text = message.toLowerCase(Locale.ROOT);
        if (containsAny(text, "\u7167\u7247", "\u56fe\u7247", "\u89c6\u9891", "\u51ed\u8bc1", "\u622a\u56fe", "photo", "video", "evidence")) {
            context.putSlot(RefundSlots.EVIDENCE, "true");
        }
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
