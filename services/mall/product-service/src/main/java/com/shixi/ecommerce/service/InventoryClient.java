package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.InventoryInitRequest;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 库存服务客户端，用于商品创建后的库存初始化。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class InventoryClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public InventoryClient(RestTemplate restTemplate, @Value("${inventory.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * 初始化库存。
     *
     * @param skuId 商品 ID
     * @param stock 初始库存
     */
    @CircuitBreaker(name = "inventoryClient", fallbackMethod = "initStockFallback")
    @Retry(name = "inventoryClient")
    @Bulkhead(name = "inventoryClient")
    public void initStock(Long skuId, Integer stock) {
        InventoryInitRequest request = new InventoryInitRequest();
        request.setSkuId(skuId);
        request.setStock(stock);
        restTemplate.postForObject(baseUrl + "/api/inventory/init", request, ApiResponse.class);
    }

    private void initStockFallback(Long skuId, Integer stock, Throwable ex) {
        throw new RuntimeException("Inventory service unavailable");
    }
}
