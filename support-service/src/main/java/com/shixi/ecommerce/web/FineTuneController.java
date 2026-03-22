package com.shixi.ecommerce.web;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.FineTuneRequest;
import com.shixi.ecommerce.dto.FineTuneResponse;
import com.shixi.ecommerce.service.FineTuneService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class FineTuneController {
    private final FineTuneService fineTuneService;

    public FineTuneController(FineTuneService fineTuneService) {
        this.fineTuneService = fineTuneService;
    }

    @PostMapping("/fine-tune")
    public ApiResponse<FineTuneResponse> createFineTune(@Valid @RequestBody FineTuneRequest request) {
        return ApiResponse.ok(fineTuneService.createJob(request));
    }
}
