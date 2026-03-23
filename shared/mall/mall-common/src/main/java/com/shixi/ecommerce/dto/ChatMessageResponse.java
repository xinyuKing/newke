package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.ChatSenderRole;
import java.time.LocalDateTime;

public class ChatMessageResponse {
    private Long id;
    private String sessionId;
    private ChatSenderRole senderRole;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;

    public ChatMessageResponse(
            Long id,
            String sessionId,
            ChatSenderRole senderRole,
            Long senderId,
            String content,
            LocalDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.senderRole = senderRole;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public ChatSenderRole getSenderRole() {
        return senderRole;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
