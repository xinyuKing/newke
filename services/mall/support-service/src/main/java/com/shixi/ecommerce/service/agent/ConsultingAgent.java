package com.shixi.ecommerce.service.agent;

import org.springframework.stereotype.Component;

@Component
public class ConsultingAgent implements Agent {
    @Override
    public String getType() {
        return "CONSULT";
    }

    @Override
    public String handle(String sessionId, String message) {
        return "Consulting: question received. Provide orderNo for faster handling.";
    }
}
