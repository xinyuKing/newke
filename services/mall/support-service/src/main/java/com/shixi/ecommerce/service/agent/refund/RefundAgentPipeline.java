package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.AgentTask;
import com.shixi.ecommerce.domain.AgentTaskStatus;
import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.dto.AgentResult;
import com.shixi.ecommerce.repository.AgentTaskRepository;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RefundAgentPipeline {
    private final RefundMasterAgent masterAgent;
    private final RefundFusionAgent fusionAgent;
    private final RefundFeedbackAgent feedbackAgent;
    private final RefundContextService contextService;
    private final AgentTaskRepository taskRepository;
    private final Executor executor;
    private final int maxLoop;
    private final Duration dedupTtl;
    private final long subAgentTimeoutMs;
    private final Map<RefundTaskType, RefundSubAgent> agentsByTask = new EnumMap<>(RefundTaskType.class);

    public RefundAgentPipeline(RefundMasterAgent masterAgent,
                               RefundFusionAgent fusionAgent,
                               RefundFeedbackAgent feedbackAgent,
                               RefundContextService contextService,
                               AgentTaskRepository taskRepository,
                               @Value("${refund.pipeline.max-loop:5}") int maxLoop,
                               @Value("${refund.pipeline.dedup-ttl-ms:120000}") long dedupTtlMs,
                               @Value("${refund.pipeline.sub-agent-timeout-ms:4000}") long subAgentTimeoutMs,
                               @Qualifier("bizExecutor") Executor executor,
                               List<RefundSubAgent> subAgents) {
        this.masterAgent = masterAgent;
        this.fusionAgent = fusionAgent;
        this.feedbackAgent = feedbackAgent;
        this.contextService = contextService;
        this.taskRepository = taskRepository;
        this.maxLoop = Math.max(1, maxLoop);
        this.dedupTtl = Duration.ofMillis(Math.max(1000L, dedupTtlMs));
        this.subAgentTimeoutMs = Math.max(500L, subAgentTimeoutMs);
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
            boolean first = contextService.recordDedup(sessionId, messageHash, dedupTtl);
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

        for (int attempt = 1; attempt <= maxLoop; attempt++) {
            List<AgentResult> results = new ArrayList<>();
            long masterStarted = System.nanoTime();
            RefundTaskPlan plan = masterAgent.plan(context);
            long masterDurationMs = elapsedMillis(masterStarted);
            String masterMeta = buildTaskMeta(plan.getMasterMeta(), AgentTaskStatus.DONE, masterDurationMs, null, false);
            results.add(new AgentResult(RefundAgentTypes.MASTER, plan.getSummary(), masterMeta));
            logTask(sessionId, RefundAgentTypes.MASTER, message, plan.getSummary(),
                    AgentTaskStatus.DONE, masterMeta, masterDurationMs);

            List<RefundSubAgent> subAgents = plan.getTasks().stream()
                    .map(agentsByTask::get)
                    .filter(agent -> agent != null)
                    .collect(Collectors.toList());

            List<AgentRun> runs = runSubAgents(subAgents, context);
            for (AgentRun run : runs) {
                RefundAgentOutput output = run.output();
                applyOutput(context, output, run.agentType());
                String meta = buildTaskMeta(output == null ? null : output.getMeta(),
                        run.status(), run.durationMs(), run.errorMessage(), run.timedOut());
                String outputText = output == null ? "" : output.getMessage();
                results.add(new AgentResult(run.agentType(), outputText, meta));
                logTask(sessionId, run.agentType(), message, outputText, run.status(), meta, run.durationMs());
            }
            contextService.save(context);

            long fusionStarted = System.nanoTime();
            FusionResult fusion = fusionAgent.fuse(context, plan, runs.stream()
                    .map(AgentRun::output).collect(Collectors.toList()));
            long fusionDurationMs = elapsedMillis(fusionStarted);
            String reply = fusion.reply();
            String fusionMeta = buildTaskMeta(fusion.meta(), AgentTaskStatus.DONE, fusionDurationMs, null, false);
            results.add(new AgentResult(RefundAgentTypes.FUSION, reply, fusionMeta));
            logTask(sessionId, RefundAgentTypes.FUSION, message, reply,
                    AgentTaskStatus.DONE, fusionMeta, fusionDurationMs);

            long feedbackStarted = System.nanoTime();
            FeedbackResult feedback = feedbackAgent.evaluate(context, plan, reply);
            long feedbackDurationMs = elapsedMillis(feedbackStarted);
            String feedbackText = feedback.isOk() ? "OK" : "FAIL: " + feedback.getReason();
            AgentTaskStatus feedbackStatus = feedback.isOk() ? AgentTaskStatus.DONE : AgentTaskStatus.FAILED;
            String feedbackMeta = buildTaskMeta(feedback.getMeta(), feedbackStatus, feedbackDurationMs,
                    feedback.isOk() ? null : feedback.getReason(), false);
            results.add(new AgentResult(RefundAgentTypes.FEEDBACK, feedbackText, feedbackMeta));
            logTask(sessionId, RefundAgentTypes.FEEDBACK, message, feedbackText,
                    feedbackStatus, feedbackMeta, feedbackDurationMs);

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
            contextService.save(context);
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
            CompletableFuture<AgentRun> future = CompletableFuture
                    .supplyAsync(() -> invokeSubAgent(agent, context), executor)
                    .completeOnTimeout(AgentRun.timeout(agent.getType(), subAgentTimeoutMs),
                            subAgentTimeoutMs, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> AgentRun.failed(agent.getType(), safeErrorMessage(ex), subAgentTimeoutMs));
            futures.add(future);
        }
        return futures.stream().map(CompletableFuture::join)
                .sorted(Comparator.comparing(AgentRun::agentType))
                .collect(Collectors.toList());
    }

    private AgentRun invokeSubAgent(RefundSubAgent agent, RefundContext context) {
        long started = System.nanoTime();
        try {
            RefundAgentOutput output = agent.handle(context);
            return AgentRun.success(agent.getType(), output, elapsedMillis(started));
        } catch (RuntimeException ex) {
            return AgentRun.failed(agent.getType(), safeErrorMessage(ex), elapsedMillis(started));
        }
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

    private void logTask(String sessionId,
                         String agentType,
                         String input,
                         String output,
                         AgentTaskStatus status,
                         String meta,
                         long durationMs) {
        AgentTask task = new AgentTask();
        task.setSessionId(sessionId);
        task.setAgentType(agentType);
        task.setInputText(truncate(input));
        task.setOutputText(truncate(output));
        task.setStatus(status);
        task.setMeta(truncateMeta(meta));
        task.setDurationMs(durationMs);
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

    private String truncateMeta(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= 2048) {
            return text;
        }
        return text.substring(0, 2048);
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private String buildTaskMeta(String sourceMeta,
                                 AgentTaskStatus status,
                                 long durationMs,
                                 String errorMessage,
                                 boolean timedOut) {
        List<String> parts = new ArrayList<>();
        if (sourceMeta != null && !sourceMeta.isBlank()) {
            parts.add(sourceMeta);
        }
        parts.add("status=" + status.name());
        parts.add("durationMs=" + durationMs);
        if (timedOut) {
            parts.add("timeout=true");
        }
        if (errorMessage != null && !errorMessage.isBlank()) {
            parts.add("error=" + errorMessage);
        }
        return String.join(" | ", parts);
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

    private String safeErrorMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return (message == null || message.isBlank()) ? cursor.getClass().getSimpleName() : message;
    }

    private record AgentRun(String agentType,
                            RefundAgentOutput output,
                            AgentTaskStatus status,
                            String errorMessage,
                            long durationMs,
                            boolean timedOut) {
        private static AgentRun success(String agentType, RefundAgentOutput output, long durationMs) {
            return new AgentRun(agentType, output, AgentTaskStatus.DONE, null, durationMs, false);
        }

        private static AgentRun failed(String agentType, String errorMessage, long durationMs) {
            RefundAgentOutput output = new RefundAgentOutput("Agent failed: " + errorMessage);
            return new AgentRun(agentType, output, AgentTaskStatus.FAILED, errorMessage, durationMs, false);
        }

        private static AgentRun timeout(String agentType, long timeoutMs) {
            RefundAgentOutput output = new RefundAgentOutput(
                    "Agent timed out after " + timeoutMs + " ms.",
                    Map.of(),
                    null,
                    "timeout=true");
            return new AgentRun(agentType, output, AgentTaskStatus.FAILED, "timeout", timeoutMs, true);
        }
    }
}
