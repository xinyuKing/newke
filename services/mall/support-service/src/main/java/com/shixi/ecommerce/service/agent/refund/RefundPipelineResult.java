package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.dto.AgentResult;
import java.util.List;

public class RefundPipelineResult {
    private final SessionState state;
    private final String reply;
    private final List<AgentResult> agentResults;

    public RefundPipelineResult(SessionState state, String reply, List<AgentResult> agentResults) {
        this.state = state;
        this.reply = reply;
        this.agentResults = agentResults;
    }

    public SessionState getState() {
        return state;
    }

    public String getReply() {
        return reply;
    }

    public List<AgentResult> getAgentResults() {
        return agentResults;
    }
}
