package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.dto.FineTuneRequest;
import com.shixi.ecommerce.repository.FineTuneJobRepository;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class FineTuneServiceTest {
    @Test
    void shouldRejectCreationWhenPipelineCommandIsMissing() {
        FineTuneJobRepository repository = mock(FineTuneJobRepository.class);
        Executor executor = Runnable::run;
        FineTuneService service = new FineTuneService(repository, executor, "   ", 30);

        FineTuneRequest request = new FineTuneRequest();
        request.setDatasetPath("D:/tmp/refund-train.jsonl");
        request.setBaseModel("qwen2.5-7b");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createJob(request));
        assertTrue(exception.getMessage().contains("Fine-tune pipeline not configured"));
        verifyNoInteractions(repository);
    }
}
