package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.config.FineTuneProperties;
import com.shixi.ecommerce.dto.FineTuneRequest;
import com.shixi.ecommerce.repository.FineTuneJobRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FineTuneServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldRejectCreationWhenPipelineCommandIsMissing() {
        FineTuneJobRepository repository = mock(FineTuneJobRepository.class);
        Executor executor = Runnable::run;
        FineTuneProperties properties = new FineTuneProperties();
        FineTuneService service = new FineTuneService(repository, executor, properties);

        FineTuneRequest request = new FineTuneRequest();
        request.setDatasetPath("D:/tmp/refund-train.jsonl");
        request.setBaseModel("qwen2.5-7b");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createJob(request));
        assertTrue(exception.getMessage().contains("Fine-tune pipeline not configured"));
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRejectDatasetOutsideAllowedRoot() throws Exception {
        FineTuneJobRepository repository = mock(FineTuneJobRepository.class);
        Executor executor = Runnable::run;

        Path allowedRoot = Files.createDirectory(tempDir.resolve("allowed"));
        Path dataset = Files.writeString(tempDir.resolve("dataset.jsonl"), "{\"text\":\"hello\"}");

        FineTuneProperties properties = new FineTuneProperties();
        properties.setExecutable("trainer");
        properties.setAllowedDatasetRoot(allowedRoot.toString());
        FineTuneService service = new FineTuneService(repository, executor, properties);

        FineTuneRequest request = new FineTuneRequest();
        request.setDatasetPath(dataset.toString());
        request.setBaseModel("qwen2.5-7b");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createJob(request));
        assertTrue(exception.getMessage().contains("outside the allowed root"));
        verifyNoInteractions(repository);
    }
}
