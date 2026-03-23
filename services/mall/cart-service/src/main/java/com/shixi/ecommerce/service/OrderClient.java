package com.shixi.ecommerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.dto.CreateOrderItemsRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * 璁㈠崟鏈嶅姟瀹㈡埛绔紝鐢ㄤ簬缁撶畻闃舵鍒涘缓璁㈠崟銆?
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class OrderClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public OrderClient(
            RestTemplate restTemplate, @Value("${order.service.url}") String baseUrl, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    /**
     * 鍒涘缓璁㈠崟锛堟壒閲忚鍗曡锛夈€?
     *
     * @param request 涓嬪崟璇锋眰
     * @return 涓嬪崟缁撴灉
     */
    @CircuitBreaker(name = "orderClient", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderClient")
    @Bulkhead(name = "orderClient")
    public CreateOrderResponse createOrder(CreateOrderItemsRequest request) {
        try {
            return restTemplate.postForObject(baseUrl + "/internal/orders", request, CreateOrderResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw translateHttpException(ex);
        }
    }

    private CreateOrderResponse createOrderFallback(CreateOrderItemsRequest request, Throwable ex) {
        BusinessException businessException = unwrapBusinessException(ex);
        if (businessException != null) {
            throw businessException;
        }
        throw new RuntimeException("Order service unavailable", ex);
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
            if (apiResponse != null
                    && apiResponse.getMessage() != null
                    && !apiResponse.getMessage().isBlank()) {
                return apiResponse.getMessage();
            }
        } catch (Exception ignored) {
        }
        return "Order request rejected";
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
