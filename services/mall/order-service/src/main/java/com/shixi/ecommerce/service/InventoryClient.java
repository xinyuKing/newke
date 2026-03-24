package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.InventoryBatchRequest;
import com.shixi.ecommerce.dto.InventoryDeductRequest;
import com.shixi.ecommerce.dto.InventoryReleaseRequest;
import com.shixi.ecommerce.dto.OrderLineItem;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 库存服务客户端，提供单品/批量库存扣减与释放能力。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class InventoryClient {
    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public InventoryClient(RestTemplate restTemplate, @Value("${inventory.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * 单品扣减库存。
     *
     * @param skuId 商品 ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    public boolean deduct(Long skuId, Integer quantity) {
        InventoryDeductRequest request = new InventoryDeductRequest();
        request.setSkuId(skuId);
        request.setQuantity(quantity);
        ApiResponse<?> response =
                restTemplate.postForObject(baseUrl + "/internal/inventory/deduct", request, ApiResponse.class);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(response.getData()));
    }

    /**
     * 批量扣减库存，减少跨服务调用次数。
     *
     * @param items 订单行
     * @return 是否扣减成功
     */
    @CircuitBreaker(name = "inventoryClient", fallbackMethod = "deductBatchFallback")
    @Bulkhead(name = "inventoryClient")
    public boolean deductBatch(List<OrderLineItem> items) {
        InventoryBatchRequest request = new InventoryBatchRequest();
        request.setItems(items.stream()
                .map(item -> {
                    InventoryDeductRequest req = new InventoryDeductRequest();
                    req.setSkuId(item.getSkuId());
                    req.setQuantity(item.getQuantity());
                    return req;
                })
                .toList());
        ApiResponse<?> response =
                restTemplate.postForObject(baseUrl + "/internal/inventory/deduct-batch", request, ApiResponse.class);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(response.getData()));
    }

    /**
     * 单品释放库存。
     *
     * @param skuId 商品 ID
     * @param quantity 释放数量
     */
    public void release(Long skuId, Integer quantity) {
        InventoryReleaseRequest request = new InventoryReleaseRequest();
        request.setSkuId(skuId);
        request.setQuantity(quantity);
        restTemplate.postForObject(baseUrl + "/internal/inventory/release", request, ApiResponse.class);
    }

    /**
     * 批量释放库存。
     *
     * @param items 订单行
     */
    @CircuitBreaker(name = "inventoryClient", fallbackMethod = "releaseBatchFallback")
    @Bulkhead(name = "inventoryClient")
    public void releaseBatch(List<OrderLineItem> items) {
        InventoryBatchRequest request = new InventoryBatchRequest();
        request.setItems(items.stream()
                .map(item -> {
                    InventoryDeductRequest req = new InventoryDeductRequest();
                    req.setSkuId(item.getSkuId());
                    req.setQuantity(item.getQuantity());
                    return req;
                })
                .toList());
        restTemplate.postForObject(baseUrl + "/internal/inventory/release-batch", request, ApiResponse.class);
    }

    private boolean deductBatchFallback(List<OrderLineItem> items, Throwable ex) {
        throw new IllegalStateException("Inventory service unavailable", ex);
    }

    private void releaseBatchFallback(List<OrderLineItem> items, Throwable ex) {
        log.error("Inventory release failed, items={}", items, ex);
        throw new IllegalStateException("Inventory compensation failed", ex);
    }
}
