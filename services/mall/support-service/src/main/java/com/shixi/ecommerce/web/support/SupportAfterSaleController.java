package com.shixi.ecommerce.web.support;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.dto.AfterSaleResponse;
import com.shixi.ecommerce.dto.AfterSaleUpdateRequest;
import com.shixi.ecommerce.service.AfterSaleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客服售后处理接口。
 *
 * @author shixi
 * @date 2026-03-20
 */
@RestController
@RequestMapping("/api/support/after-sale")
public class SupportAfterSaleController {
    private final AfterSaleService afterSaleService;

    public SupportAfterSaleController(AfterSaleService afterSaleService) {
        this.afterSaleService = afterSaleService;
    }

    @GetMapping
    public ApiResponse<List<AfterSaleResponse>> list(@RequestParam(required = false) AfterSaleStatus status) {
        return ApiResponse.ok(afterSaleService.listAll(status));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AfterSaleResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody AfterSaleUpdateRequest request) {
        return ApiResponse.ok(afterSaleService.updateStatus(id, request.getStatus()));
    }
}
