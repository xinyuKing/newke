package com.shixi.ecommerce.web;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.RefundIntentTrainRequest;
import com.shixi.ecommerce.dto.RefundIntentTrainResponse;
import com.shixi.ecommerce.service.agent.refund.RefundIntentTrainingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/refund/intent")
public class RefundIntentController {
    private final RefundIntentTrainingService trainingService;

    public RefundIntentController(RefundIntentTrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/train")
    public ApiResponse<RefundIntentTrainResponse> train(@Valid @RequestBody RefundIntentTrainRequest request) {
        return ApiResponse.ok(trainingService.train(request));
    }
}
