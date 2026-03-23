package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.config.FineTuneProperties;
import com.shixi.ecommerce.domain.FineTuneJob;
import com.shixi.ecommerce.domain.FineTuneStatus;
import com.shixi.ecommerce.dto.FineTuneRequest;
import com.shixi.ecommerce.dto.FineTuneResponse;
import com.shixi.ecommerce.repository.FineTuneJobRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class FineTuneService {
    private static final Logger logger = LoggerFactory.getLogger(FineTuneService.class);
    private static final int MAX_OUTPUT_LENGTH = 800;
    private static final String CONFIG_ERROR_MESSAGE =
            "Fine-tune pipeline not configured. Set ai.fine-tune.executable first.";
    private static final String JOB_ID_ENV = "NEWKE_FINE_TUNE_JOB_ID";
    private static final String DATASET_PATH_ENV = "NEWKE_FINE_TUNE_DATASET_PATH";
    private static final String BASE_MODEL_ENV = "NEWKE_FINE_TUNE_BASE_MODEL";

    private final FineTuneJobRepository repository;
    private final Executor executor;
    private final FineTuneProperties properties;

    public FineTuneService(
            FineTuneJobRepository repository,
            @Qualifier("bizExecutor") Executor executor,
            FineTuneProperties properties) {
        this.repository = repository;
        this.executor = executor;
        this.properties = properties;
    }

    public FineTuneResponse createJob(FineTuneRequest request) {
        if (!properties.isConfigured()) {
            throw new BusinessException(CONFIG_ERROR_MESSAGE);
        }
        ResolvedFineTuneRequest resolvedRequest = resolveRequest(request);
        FineTuneJob job = new FineTuneJob();
        job.setJobId(generateJobId());
        job.setDatasetPath(resolvedRequest.datasetPath());
        job.setBaseModel(resolvedRequest.baseModel());
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
            processBuilder.environment().put(JOB_ID_ENV, job.getJobId());
            processBuilder.environment().put(DATASET_PATH_ENV, job.getDatasetPath());
            processBuilder.environment().put(BASE_MODEL_ENV, job.getBaseModel());
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(logFile.toFile());
            process = processBuilder.start();
            boolean finished = process.waitFor(Math.max(1L, properties.getTimeoutMinutes()), TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Fine-tune process timed out after "
                        + Math.max(1L, properties.getTimeoutMinutes()) + " minute(s).");
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
        List<String> command = new ArrayList<>();
        command.add(properties.getExecutable().trim());
        for (String arg : properties.getArgs()) {
            if (arg == null || arg.isBlank()) {
                continue;
            }
            command.add(arg.replace("{jobId}", job.getJobId())
                    .replace("{datasetPath}", job.getDatasetPath())
                    .replace("{baseModel}", job.getBaseModel()));
        }
        return command;
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

    private ResolvedFineTuneRequest resolveRequest(FineTuneRequest request) {
        return new ResolvedFineTuneRequest(
                resolveDatasetPath(request.getDatasetPath()), resolveBaseModel(request.getBaseModel()));
    }

    private String resolveDatasetPath(String datasetPath) {
        if (datasetPath == null || datasetPath.isBlank()) {
            throw new BusinessException("Invalid fine-tune dataset path");
        }
        Path path = toRealPath(datasetPath, "Invalid fine-tune dataset path");
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new BusinessException("Fine-tune dataset must be a readable file");
        }

        String allowedRootText = properties.getAllowedDatasetRoot();
        if (allowedRootText == null || allowedRootText.isBlank()) {
            return path.toString();
        }

        Path allowedRoot = toRealPath(allowedRootText, "Fine-tune dataset root is misconfigured");
        if (!path.startsWith(allowedRoot)) {
            throw new BusinessException("Fine-tune dataset path is outside the allowed root");
        }
        return path.toString();
    }

    private String resolveBaseModel(String baseModel) {
        if (baseModel == null || baseModel.isBlank() || !baseModel.matches("[A-Za-z0-9._:-]{1,100}")) {
            throw new BusinessException("Invalid fine-tune base model");
        }
        return baseModel;
    }

    private Path toRealPath(String rawPath, String errorMessage) {
        try {
            return Path.of(rawPath).toRealPath();
        } catch (IOException | InvalidPathException ex) {
            throw new BusinessException(errorMessage);
        }
    }

    private record ResolvedFineTuneRequest(String datasetPath, String baseModel) {}
}
