package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.IntentType;
import com.shixi.ecommerce.domain.SessionState;

import java.util.List;

public class AgentChatResponse {
    private String sessionId;
    private SessionState state;
    private IntentType intent;
    private String reply;
    private List<AgentResult> agentResults;

    public AgentChatResponse(String sessionId, SessionState state, IntentType intent, String reply, List<AgentResult> agentResults) {
        this.sessionId = sessionId;
        this.state = state;
        this.intent = intent;
        this.reply = reply;
        this.agentResults = agentResults;
    }

    public String getSessionId() {
        return sessionId;
    }

    public SessionState getState() {
        return state;
    }

    public IntentType getIntent() {
        return intent;
    }

    public String getReply() {
        return reply;
    }

    public List<AgentResult> getAgentResults() {
        return agentResults;
    }
}
