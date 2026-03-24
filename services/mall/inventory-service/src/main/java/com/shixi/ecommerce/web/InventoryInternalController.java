package com.shixi.ecommerce.web;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.InventoryBatchRequest;
import com.shixi.ecommerce.dto.InventoryDeductRequest;
import com.shixi.ecommerce.dto.InventoryInitRequest;
import com.shixi.ecommerce.dto.InventoryReleaseRequest;
import com.shixi.ecommerce.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存内部接口，供订单等服务调用。
 *
 * @author shixi
 * @date 2026-03-20
 */
@RestController
@RequestMapping("/internal/inventory")
public class InventoryInternalController {
    private final InventoryService inventoryService;

    public InventoryInternalController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/init")
    public ApiResponse<String> init(@Valid @RequestBody InventoryInitRequest request) {
        inventoryService.initStock(request.getSkuId(), request.getStock());
        return ApiResponse.ok("OK");
    }

    @DeleteMapping("/{skuId}")
    public ApiResponse<String> delete(@PathVariable Long skuId) {
        inventoryService.deleteStock(skuId);
        return ApiResponse.ok("OK");
    }

    /**
     * 单品扣减库存。
     *
     * @param request 扣减请求
     * @return 是否扣减成功
     */
    @PostMapping("/deduct")
    public ApiResponse<Boolean> deduct(@Valid @RequestBody InventoryDeductRequest request) {
        boolean result = inventoryService.deductStock(request.getSkuId(), request.getQuantity());
        return ApiResponse.ok(result);
    }

    /**
     * 批量扣减库存。
     *
     * @param request 扣减请求
     * @return 是否扣减成功
     */
    @PostMapping("/deduct-batch")
    public ApiResponse<Boolean> deductBatch(@Valid @RequestBody InventoryBatchRequest request) {
        boolean result = inventoryService.deductBatch(request.getItems());
        return ApiResponse.ok(result);
    }

    /**
     * 单品释放库存。
     *
     * @param request 释放请求
     * @return 操作结果
     */
    @PostMapping("/release")
    public ApiResponse<String> release(@Valid @RequestBody InventoryReleaseRequest request) {
        inventoryService.releaseStock(request.getSkuId(), request.getQuantity());
        return ApiResponse.ok("OK");
    }

    /**
     * 批量释放库存。
     *
     * @param request 释放请求
     * @return 操作结果
     */
    @PostMapping("/release-batch")
    public ApiResponse<String> releaseBatch(@Valid @RequestBody InventoryBatchRequest request) {
        inventoryService.releaseBatch(request.getItems());
        return ApiResponse.ok("OK");
    }
}
