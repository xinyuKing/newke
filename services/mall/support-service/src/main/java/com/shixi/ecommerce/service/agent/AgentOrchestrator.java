package com.shixi.ecommerce.service.agent;

import com.shixi.ecommerce.domain.IntentType;
import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.dto.AgentChatRequest;
import com.shixi.ecommerce.dto.AgentChatResponse;
import com.shixi.ecommerce.dto.AgentResult;
import com.shixi.ecommerce.service.agent.refund.RefundAgentPipeline;
import com.shixi.ecommerce.service.agent.refund.RefundPipelineResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentOrchestrator {
    private final IntentRecognizer intentRecognizer;
    private final AgentSessionService sessionService;
    private final RefundAgentPipeline refundAgentPipeline;

    public AgentOrchestrator(IntentRecognizer intentRecognizer,
                             AgentSessionService sessionService,
                             RefundAgentPipeline refundAgentPipeline) {
        this.intentRecognizer = intentRecognizer;
        this.sessionService = sessionService;
        this.refundAgentPipeline = refundAgentPipeline;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        IntentType intent = intentRecognizer.recognize(request.getMessage());
        String sessionId = sessionService.getOrCreate(request.getSessionId(), intent).getSessionId();

        if (intent != IntentType.REFUND) {
            sessionService.updateState(sessionId, SessionState.HANDOFF, intent);
            String reply = "Refund multi-agent only handles refund requests. Transfer to human support.";
            return new AgentChatResponse(sessionId, SessionState.HANDOFF, intent, reply, List.of());
        }

        RefundPipelineResult result = refundAgentPipeline.handle(sessionId, request.getMessage());
        sessionService.updateState(sessionId, result.getState(), intent);
        return new AgentChatResponse(sessionId, result.getState(), intent, result.getReply(), result.getAgentResults());
    }
}
