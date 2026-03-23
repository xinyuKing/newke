package com.shixi.ecommerce.web.admin;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.AdminRiskProductResponse;
import com.shixi.ecommerce.service.AdminReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {
    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping("/risk")
    public ApiResponse<List<AdminRiskProductResponse>> listRisk() {
        return ApiResponse.ok(adminReviewService.listRiskProducts());
    }
}
