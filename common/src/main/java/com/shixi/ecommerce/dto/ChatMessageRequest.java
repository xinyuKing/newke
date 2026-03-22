package com.shixi.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatMessageRequest {
    @NotBlank
    private String sessionId;

    @NotBlank
    private String content;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
