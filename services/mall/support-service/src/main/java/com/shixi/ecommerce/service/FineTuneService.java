package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.FineTuneJob;
import com.shixi.ecommerce.domain.FineTuneStatus;
import com.shixi.ecommerce.dto.FineTuneRequest;
import com.shixi.ecommerce.dto.FineTuneResponse;
import com.shixi.ecommerce.repository.FineTuneJobRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FineTuneService {
    private static final Logger logger = LoggerFactory.getLogger(FineTuneService.class);
    private static final int MAX_OUTPUT_LENGTH = 800;

    private final FineTuneJobRepository repository;
    private final Executor executor;
    private final String commandTemplate;
    private final long timeoutMinutes;

    public FineTuneService(
            FineTuneJobRepository repository,
            @Qualifier("bizExecutor") Executor executor,
            @Value("${ai.fine-tune.command:}") String commandTemplate,
            @Value("${ai.fine-tune.timeout-minutes:120}") long timeoutMinutes) {
        this.repository = repository;
        this.executor = executor;
        this.commandTemplate = commandTemplate;
        this.timeoutMinutes = Math.max(1L, timeoutMinutes);
    }

    public FineTuneResponse createJob(FineTuneRequest request) {
        if (commandTemplate == null || commandTemplate.isBlank()) {
            throw new BusinessException("Fine-tune pipeline not configured. Set ai.fine-tune.command first.");
        }
        FineTuneJob job = new FineTuneJob();
        job.setJobId(generateJobId());
        job.setDatasetPath(request.getDatasetPath());
        job.setBaseModel(request.getBaseModel());
        job.setStatus(FineTuneStatus.QUEUED);
        repository.save(job);

        executor.execute(() -> runJob(job.getJobId()));
        return new FineTuneResponse(job.getJobId(), job.getStatus());
    }

    private void runJob(String jobId) {
        repository.findByJobId(jobId).ifPresent(job -> {
            try {
                job.setStatus(FineTuneStatus.RUNNING);
                repository.save(job);
                executePipeline(job);
                job.setStatus(FineTuneStatus.SUCCEEDED);
            } catch (Exception ex) {
                logger.warn("fine-tune job {} failed: {}", jobId, ex.getMessage());
                job.setStatus(FineTuneStatus.FAILED);
            } finally {
                repository.save(job);
            }
        });
    }

    private void executePipeline(FineTuneJob job) throws Exception {
        Path logFile = Files.createTempFile("fine-tune-" + job.getJobId(), ".log");
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(job));
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(logFile.toFile());
            process = processBuilder.start();
            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Fine-tune process timed out after " + timeoutMinutes + " minute(s).");
            }
            if (process.exitValue() != 0) {
                String output = Files.readString(logFile, StandardCharsets.UTF_8);
                throw new IllegalStateException(
                        "Fine-tune process exited with code " + process.exitValue() + ". " + trimOutput(output));
            }
        } finally {
            Files.deleteIfExists(logFile);
        }
    }

    private List<String> buildCommand(FineTuneJob job) {
        String resolved = commandTemplate
                .replace("{jobId}", job.getJobId())
                .replace("{datasetPath}", job.getDatasetPath())
                .replace("{baseModel}", job.getBaseModel());
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return List.of("cmd.exe", "/c", resolved);
        }
        return List.of("/bin/sh", "-c", resolved);
    }

    private String trimOutput(String output) {
        if (output == null || output.isBlank()) {
            return "No process output was captured.";
        }
        String trimmed = output.trim();
        if (trimmed.length() <= MAX_OUTPUT_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_OUTPUT_LENGTH) + "...";
    }

    private String generateJobId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
