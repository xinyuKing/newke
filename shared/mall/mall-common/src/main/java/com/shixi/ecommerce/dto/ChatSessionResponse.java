package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.ChatSessionStatus;

import java.time.LocalDateTime;

public class ChatSessionResponse {
    private String sessionId;
    private Long userId;
    private Long supportId;
    private ChatSessionStatus status;
    private LocalDateTime updatedAt;

    public ChatSessionResponse(String sessionId, Long userId, Long supportId, ChatSessionStatus status, LocalDateTime updatedAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.supportId = supportId;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSupportId() {
        return supportId;
    }

    public ChatSessionStatus getStatus() {
        return status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
