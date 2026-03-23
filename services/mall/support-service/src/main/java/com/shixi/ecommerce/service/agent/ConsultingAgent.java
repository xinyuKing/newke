package com.shixi.ecommerce.service.agent;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ConsultingAgent implements Agent {
    @Override
    public String getType() {
        return "CONSULT";
    }

    @Override
    public String handle(String sessionId, String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (containsAny(text, "price", "discount", "优惠", "价格")) {
            return "Consulting: this looks like a pricing or promotion question. "
                    + "Please provide the SKU or product link so we can confirm the latest price and discounts.";
        }
        if (containsAny(text, "stock", "inventory", "库存", "现货")) {
            return "Consulting: this looks like an inventory question. "
                    + "Please provide the SKU or product link so we can check stock and restock timing.";
        }
        if (containsAny(text, "发货", "到货", "delivery", "shipping")) {
            return "Consulting: this looks like a shipping inquiry. "
                    + "Please share the product and destination so we can estimate dispatch and delivery timing.";
        }
        return "Consulting: question received. Please provide the product link, SKU, or order number for faster handling.";
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
