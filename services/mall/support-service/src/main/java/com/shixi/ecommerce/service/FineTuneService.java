package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.FineTuneJob;
import com.shixi.ecommerce.domain.FineTuneStatus;
import com.shixi.ecommerce.dto.FineTuneRequest;
import com.shixi.ecommerce.dto.FineTuneResponse;
import com.shixi.ecommerce.repository.FineTuneJobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class FineTuneService {
    private final FineTuneJobRepository repository;
    private final Executor executor;

    public FineTuneService(FineTuneJobRepository repository,
                           @Qualifier("bizExecutor") Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    public FineTuneResponse createJob(FineTuneRequest request) {
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
                // TODO: integrate real fine-tuning pipeline (LoRA/QLoRA + training script)
                job.setStatus(FineTuneStatus.SUCCEEDED);
                repository.save(job);
            } catch (RuntimeException ex) {
                job.setStatus(FineTuneStatus.FAILED);
                repository.save(job);
            }
        });
    }

    private String generateJobId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
