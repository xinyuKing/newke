package com.shixi.ecommerce.web.merchant;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.MerchantAnalysisResponse;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.MerchantAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/analysis")
public class MerchantAnalysisController {
    private final MerchantAnalysisService merchantAnalysisService;
    private final CurrentUserService currentUserService;

    public MerchantAnalysisController(MerchantAnalysisService merchantAnalysisService,
                                      CurrentUserService currentUserService) {
        this.merchantAnalysisService = merchantAnalysisService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<MerchantAnalysisResponse> analyze() {
        Long merchantId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(merchantAnalysisService.analyze(merchantId));
    }
}
