package com.shixi.ecommerce.web;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.InventoryInitRequest;
import com.shixi.ecommerce.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/init")
    public ApiResponse<String> initStock(@Valid @RequestBody InventoryInitRequest request) {
        inventoryService.initStock(request.getSkuId(), request.getStock());
        return ApiResponse.ok("OK");
    }
}
