package com.shixi.ecommerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.dto.InventoryInitRequest;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
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
    private final ObjectMapper objectMapper;

    public InventoryClient(
            RestTemplate restTemplate, @Value("${inventory.service.url}") String baseUrl, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
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
        try {
            ApiResponse<?> response =
                    restTemplate.postForObject(baseUrl + "/internal/inventory/init", request, ApiResponse.class);
            if (response == null) {
                throw new IllegalStateException("Inventory init failed");
            }
            if (!response.isSuccess()) {
                throw new BusinessException(resolveMessage(response.getMessage(), "Inventory init rejected"));
            }
        } catch (HttpStatusCodeException ex) {
            throw translateHttpException(ex);
        }
    }

    public void deleteStock(Long skuId) {
        restTemplate.delete(baseUrl + "/internal/inventory/{skuId}", skuId);
    }

    private void initStockFallback(Long skuId, Integer stock, Throwable ex) {
        BusinessException businessException = unwrapBusinessException(ex);
        if (businessException != null) {
            throw businessException;
        }
        throw new RuntimeException("Inventory service unavailable", ex);
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
