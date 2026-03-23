package com.shixi.ecommerce.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_task", indexes = {
        @Index(name = "idx_agent_task_session", columnList = "sessionId")
})
public class AgentTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 32)
    private String agentType;

    @Column(nullable = false, length = 1024)
    private String inputText;

    @Column(length = 1024)
    private String outputText;

    @Column(length = 2048)
    private String meta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AgentTaskStatus status;

    private Long durationMs;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
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

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getInputText() {
        return inputText;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public String getOutputText() {
        return outputText;
    }

    public void setOutputText(String outputText) {
        this.outputText = outputText;
    }

    public AgentTaskStatus getStatus() {
        return status;
    }

    public void setStatus(AgentTaskStatus status) {
        this.status = status;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
