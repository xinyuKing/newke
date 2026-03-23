package com.shixi.ecommerce.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_session", indexes = {
        @Index(name = "idx_agent_session_id", columnList = "sessionId", unique = true)
})
public class AgentSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SessionState state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IntentType lastIntent;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public IntentType getLastIntent() {
        return lastIntent;
    }

    public void setLastIntent(IntentType lastIntent) {
        this.lastIntent = lastIntent;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
