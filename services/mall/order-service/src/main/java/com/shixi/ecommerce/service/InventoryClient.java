package com.shixi.ecommerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.common.BusinessException;
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
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
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
    private final ObjectMapper objectMapper;

    public InventoryClient(
            RestTemplate restTemplate, @Value("${inventory.service.url}") String baseUrl, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
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
        try {
            ApiResponse<?> response =
                    restTemplate.postForObject(baseUrl + "/internal/inventory/deduct", request, ApiResponse.class);
            return parseDeductResponse(response);
        } catch (HttpStatusCodeException ex) {
            throw translateHttpException(ex);
        }
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
        try {
            ApiResponse<?> response = restTemplate.postForObject(
                    baseUrl + "/internal/inventory/deduct-batch", request, ApiResponse.class);
            return parseDeductResponse(response);
        } catch (HttpStatusCodeException ex) {
            throw translateHttpException(ex);
        }
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
        try {
            ApiResponse<?> response =
                    restTemplate.postForObject(baseUrl + "/internal/inventory/release", request, ApiResponse.class);
            validateCompensationResponse(response);
        } catch (HttpStatusCodeException ex) {
            throw translateHttpException(ex);
        }
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
        try {
            ApiResponse<?> response = restTemplate.postForObject(
                    baseUrl + "/internal/inventory/release-batch", request, ApiResponse.class);
            validateCompensationResponse(response);
        } catch (HttpStatusCodeException ex) {
            throw translateHttpException(ex);
        }
    }

    private boolean deductBatchFallback(List<OrderLineItem> items, Throwable ex) {
        BusinessException businessException = unwrapBusinessException(ex);
        if (businessException != null) {
            throw businessException;
        }
        throw new IllegalStateException("Inventory service unavailable", ex);
    }

    private void releaseBatchFallback(List<OrderLineItem> items, Throwable ex) {
        log.error("Inventory release failed, items={}", items, ex);
        throw new IllegalStateException("Inventory compensation failed", ex);
    }

    private boolean parseDeductResponse(ApiResponse<?> response) {
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Inventory service unavailable");
        }
        if (!response.isSuccess()) {
            throw new BusinessException(resolveMessage(response.getMessage(), "Inventory request rejected"));
        }
        return Boolean.parseBoolean(String.valueOf(response.getData()));
    }

    private void validateCompensationResponse(ApiResponse<?> response) {
        if (response == null) {
            throw new IllegalStateException("Inventory compensation failed");
        }
        if (!response.isSuccess()) {
            throw new BusinessException(resolveMessage(response.getMessage(), "Inventory compensation rejected"));
        }
    }

    private RuntimeException translateHttpException(HttpStatusCodeException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();
        if (statusCode.is4xxClientError()) {
            return new BusinessException(extractBusinessMessage(ex));
        }
        return ex;
    }

    private String extractBusinessMessage(HttpStatusCodeException ex) {
        try {
            ApiResponse<?> apiResponse = objectMapper.readValue(ex.getResponseBodyAsByteArray(), ApiResponse.class);
            if (apiResponse != null) {
                return resolveMessage(apiResponse.getMessage(), "Inventory request rejected");
            }
        } catch (Exception ignored) {
        }
        return "Inventory request rejected";
    }

    private String resolveMessage(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        return message;
    }

    private BusinessException unwrapBusinessException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException;
            }
            current = current.getCause();
        }
        return null;
    }
}
