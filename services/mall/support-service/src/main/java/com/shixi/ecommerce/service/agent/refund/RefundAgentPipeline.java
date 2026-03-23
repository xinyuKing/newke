package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.AgentTask;
import com.shixi.ecommerce.domain.AgentTaskStatus;
import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.dto.AgentResult;
import com.shixi.ecommerce.repository.AgentTaskRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class RefundAgentPipeline {
    private static final int MAX_LOOP = 5;
    private static final Duration DEDUP_TTL = Duration.ofMinutes(2);

    private final RefundMasterAgent masterAgent;
    private final RefundFusionAgent fusionAgent;
    private final RefundFeedbackAgent feedbackAgent;
    private final RefundContextService contextService;
    private final AgentTaskRepository taskRepository;
    private final Executor executor;
    private final Map<RefundTaskType, RefundSubAgent> agentsByTask = new EnumMap<>(RefundTaskType.class);

    public RefundAgentPipeline(RefundMasterAgent masterAgent,
                               RefundFusionAgent fusionAgent,
                               RefundFeedbackAgent feedbackAgent,
                               RefundContextService contextService,
                               AgentTaskRepository taskRepository,
                               @Qualifier("bizExecutor") Executor executor,
                               List<RefundSubAgent> subAgents) {
        this.masterAgent = masterAgent;
        this.fusionAgent = fusionAgent;
        this.feedbackAgent = feedbackAgent;
        this.contextService = contextService;
        this.taskRepository = taskRepository;
        this.executor = executor;
        for (RefundSubAgent agent : subAgents) {
            RefundTaskType taskType = mapTaskType(agent.getType());
            if (taskType != null) {
                agentsByTask.put(taskType, agent);
            }
        }
    }

    public RefundPipelineResult handle(String sessionId, String message) {
        String messageHash = hashMessage(message);
        if (messageHash != null) {
            boolean first = contextService.recordDedup(sessionId, messageHash, DEDUP_TTL);
            if (!first) {
                RefundContext cached = contextService.load(sessionId);
                String reply = "We received the same message recently. Please wait for the previous response.";
                SessionState state = cached.getState() == null ? SessionState.INIT : cached.getState();
                return new RefundPipelineResult(state, reply, List.of());
            }
        }
        RefundContext context = contextService.load(sessionId);
        context.setSessionId(sessionId);
        context.setMessage(message);

        for (int attempt = 1; attempt <= MAX_LOOP; attempt++) {
            List<AgentResult> results = new ArrayList<>();
            RefundTaskPlan plan = masterAgent.plan(context);
            results.add(new AgentResult(RefundAgentTypes.MASTER, plan.getSummary(), plan.getMasterMeta()));
            logTask(sessionId, RefundAgentTypes.MASTER, message, plan.getSummary(), AgentTaskStatus.DONE);

            List<RefundSubAgent> subAgents = plan.getTasks().stream()
                    .map(agentsByTask::get)
                    .filter(agent -> agent != null)
                    .collect(Collectors.toList());

            List<AgentRun> runs = runSubAgents(subAgents, context);
            for (AgentRun run : runs) {
                RefundAgentOutput output = run.output();
                applyOutput(context, output, run.agentType());
                String meta = output == null ? null : output.getMeta();
                String outputText = output == null ? "" : output.getMessage();
                results.add(new AgentResult(run.agentType(), outputText, meta));
                logTask(sessionId, run.agentType(), message, outputText, AgentTaskStatus.DONE);
            }

            FusionResult fusion = fusionAgent.fuse(context, plan, runs.stream()
                    .map(AgentRun::output).collect(Collectors.toList()));
            String reply = fusion.reply();
            results.add(new AgentResult(RefundAgentTypes.FUSION, reply, fusion.meta()));
            logTask(sessionId, RefundAgentTypes.FUSION, message, reply, AgentTaskStatus.DONE);

            FeedbackResult feedback = feedbackAgent.evaluate(context, plan, reply);
            String feedbackText = feedback.isOk() ? "OK" : "FAIL: " + feedback.getReason();
            results.add(new AgentResult(RefundAgentTypes.FEEDBACK, feedbackText, feedback.getMeta()));
            logTask(sessionId, RefundAgentTypes.FEEDBACK, message, feedbackText,
                    feedback.isOk() ? AgentTaskStatus.DONE : AgentTaskStatus.FAILED);

            if (feedback.isOk()) {
                SessionState finalState = resolveFinalState(plan.getNextState(), context.getDecision());
                context.setState(finalState);
                contextService.save(context);
                if (finalState == SessionState.APPROVED || finalState == SessionState.REJECTED
                        || finalState == SessionState.HANDOFF) {
                    contextService.clear(sessionId);
                }
                return new RefundPipelineResult(finalState, reply, results);
            }

            context.addFeedbackHint(feedback.getReason());
        }

        context.setState(SessionState.HANDOFF);
        contextService.save(context);
        contextService.clear(sessionId);
        String reply = "Refund assistant could not resolve after retries. Transfer to human support.";
        return new RefundPipelineResult(SessionState.HANDOFF, reply, List.of(
                new AgentResult(RefundAgentTypes.FEEDBACK, "FAIL: max retries reached", null)));
    }

    private List<AgentRun> runSubAgents(List<RefundSubAgent> subAgents, RefundContext context) {
        if (subAgents.isEmpty()) {
            return List.of();
        }
        List<CompletableFuture<AgentRun>> futures = new ArrayList<>();
        for (RefundSubAgent agent : subAgents) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                RefundAgentOutput output = agent.handle(context);
                return new AgentRun(agent.getType(), output);
            }, executor));
        }
        return futures.stream().map(CompletableFuture::join)
                .sorted(Comparator.comparing(AgentRun::agentType))
                .collect(Collectors.toList());
    }

    private void applyOutput(RefundContext context, RefundAgentOutput output, String agentType) {
        if (output == null) {
            return;
        }
        if (output.getUpdates() != null) {
            output.getUpdates().forEach(context::putSlot);
        }
        RefundDecision decision = output.getDecision();
        if (decision == null) {
            return;
        }
        if (RefundAgentTypes.POLICY.equals(agentType)) {
            context.setDecision(decision);
            return;
        }
        if (RefundAgentTypes.RISK.equals(agentType) && decision == RefundDecision.MANUAL_REVIEW) {
            RefundDecision current = context.getDecision();
            if (current == null || current == RefundDecision.APPROVE || current == RefundDecision.NEED_INFO) {
                context.setDecision(RefundDecision.MANUAL_REVIEW);
            }
        }
    }

    private SessionState resolveFinalState(SessionState planned, RefundDecision decision) {
        if (planned == SessionState.REVIEWING) {
            if (decision == RefundDecision.APPROVE) {
                return SessionState.APPROVED;
            }
            if (decision == RefundDecision.REJECT) {
                return SessionState.REJECTED;
            }
        }
        return planned;
    }

    private RefundTaskType mapTaskType(String agentType) {
        if (RefundAgentTypes.ORDER.equals(agentType)) {
            return RefundTaskType.ASK_ORDER;
        }
        if (RefundAgentTypes.REASON.equals(agentType)) {
            return RefundTaskType.ASK_REASON;
        }
        if (RefundAgentTypes.EVIDENCE.equals(agentType)) {
            return RefundTaskType.ASK_EVIDENCE;
        }
        if (RefundAgentTypes.POLICY.equals(agentType)) {
            return RefundTaskType.POLICY_CHECK;
        }
        if (RefundAgentTypes.LOGISTICS.equals(agentType)) {
            return RefundTaskType.LOGISTICS_CHECK;
        }
        if (RefundAgentTypes.RISK.equals(agentType)) {
            return RefundTaskType.RISK_CHECK;
        }
        return null;
    }

    private void logTask(String sessionId, String agentType, String input, String output, AgentTaskStatus status) {
        AgentTask task = new AgentTask();
        task.setSessionId(sessionId);
        task.setAgentType(agentType);
        task.setInputText(truncate(input));
        task.setOutputText(truncate(output));
        task.setStatus(status);
        taskRepository.save(task);
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= 1024) {
            return text;
        }
        return text.substring(0, 1024);
    }

    private String hashMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private record AgentRun(String agentType, RefundAgentOutput output) {
    }
}
