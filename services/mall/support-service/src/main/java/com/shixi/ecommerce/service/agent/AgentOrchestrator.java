package com.shixi.ecommerce.service.agent;

import com.shixi.ecommerce.domain.IntentType;
import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.dto.AgentChatRequest;
import com.shixi.ecommerce.dto.AgentChatResponse;
import com.shixi.ecommerce.dto.AgentResult;
import com.shixi.ecommerce.service.agent.refund.RefundAgentPipeline;
import com.shixi.ecommerce.service.agent.refund.RefundPipelineResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentOrchestrator {
    private static final String REFUND_REVIEW = "REFUND_REVIEW";

    private final IntentRecognizer intentRecognizer;
    private final AgentSessionService sessionService;
    private final RefundAgentPipeline refundAgentPipeline;
    private final Map<String, Agent> agentsByType = new HashMap<>();

    public AgentOrchestrator(IntentRecognizer intentRecognizer,
                             AgentSessionService sessionService,
                             RefundAgentPipeline refundAgentPipeline,
                             List<Agent> agents) {
        this.intentRecognizer = intentRecognizer;
        this.sessionService = sessionService;
        this.refundAgentPipeline = refundAgentPipeline;
        for (Agent agent : agents) {
            agentsByType.put(agent.getType(), agent);
        }
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        IntentType intent = intentRecognizer.recognize(request.getMessage());
        String sessionId = sessionService.getOrCreate(request.getSessionId(), intent).getSessionId();

        if (intent == IntentType.REFUND) {
            return handleRefund(sessionId, intent, request.getMessage());
        }

        Agent directAgent = agentsByType.get(intent.name());
        if (directAgent != null) {
            return handleDirectAgent(sessionId, intent, directAgent, request.getMessage());
        }

        sessionService.updateState(sessionId, SessionState.HANDOFF, intent);
        String reply = "Current automation supports refund, logistics, and basic consulting requests. Transfer to human support.";
        AgentResult routerResult = new AgentResult("ROUTER", "handoff", "intent=" + intent.name());
        return new AgentChatResponse(sessionId, SessionState.HANDOFF, intent, reply, List.of(routerResult));
    }

    private AgentChatResponse handleRefund(String sessionId, IntentType intent, String message) {
        try {
            RefundPipelineResult result = refundAgentPipeline.handle(sessionId, message);
            sessionService.updateState(sessionId, result.getState(), intent);
            return new AgentChatResponse(sessionId, result.getState(), intent, result.getReply(), result.getAgentResults());
        } catch (RuntimeException ex) {
            Agent fallbackAgent = agentsByType.get(REFUND_REVIEW);
            List<AgentResult> results = new ArrayList<>();
            String reply = "Refund workflow is temporarily degraded. Transfer to human support.";
            if (fallbackAgent != null) {
                try {
                    reply = fallbackAgent.handle(sessionId, message)
                            + " If the case is still unclear, human support will continue the review.";
                    results.add(new AgentResult(
                            fallbackAgent.getType(),
                            reply,
                            "fallback=true,error=" + safeErrorMessage(ex)));
                } catch (RuntimeException fallbackEx) {
                    results.add(new AgentResult(
                            fallbackAgent.getType(),
                            "handoff",
                            "fallback=true,error=" + safeErrorMessage(fallbackEx)));
                }
            }
            sessionService.updateState(sessionId, SessionState.HANDOFF, intent);
            return new AgentChatResponse(sessionId, SessionState.HANDOFF, intent, reply, results);
        }
    }

    private AgentChatResponse handleDirectAgent(String sessionId,
                                                IntentType intent,
                                                Agent agent,
                                                String message) {
        try {
            String reply = agent.handle(sessionId, message);
            sessionService.updateState(sessionId, SessionState.DONE, intent);
            AgentResult result = new AgentResult(agent.getType(), reply, "route=direct");
            return new AgentChatResponse(sessionId, SessionState.DONE, intent, reply, List.of(result));
        } catch (RuntimeException ex) {
            sessionService.updateState(sessionId, SessionState.HANDOFF, intent);
            String reply = "Automatic handling failed for this request. Transfer to human support.";
            AgentResult result = new AgentResult(
                    agent.getType(),
                    "handoff",
                    "route=direct-failed,error=" + safeErrorMessage(ex));
            return new AgentChatResponse(sessionId, SessionState.HANDOFF, intent, reply, List.of(result));
        }
    }

    private String safeErrorMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return (message == null || message.isBlank()) ? cursor.getClass().getSimpleName() : message;
    }
}
