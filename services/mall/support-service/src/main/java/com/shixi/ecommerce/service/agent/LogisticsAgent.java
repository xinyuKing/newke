package com.shixi.ecommerce.service.agent;

import org.springframework.stereotype.Component;

@Component
public class LogisticsAgent implements Agent {
    @Override
    public String getType() {
        return "LOGISTICS";
    }

    @Override
    public String handle(String sessionId, String message) {
        return "Logistics: tracking requested. If no update within 48 hours, request manual intervention.";
    }
}
