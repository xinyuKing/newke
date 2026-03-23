package com.shixi.ecommerce.service.agent;

public interface Agent {
    String getType();

    String handle(String sessionId, String message);
}
