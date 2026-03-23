package com.shixi.ecommerce.service;

import com.shixi.ecommerce.dto.CreateOrderItemsRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 订单服务客户端，用于结算阶段创建订单。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class OrderClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrderClient(RestTemplate restTemplate,
                       @Value("${order.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * 创建订单（批量订单行）。
     *
     * @param request 下单请求
     * @return 下单结果
     */
    @CircuitBreaker(name = "orderClient", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderClient")
    @Bulkhead(name = "orderClient")
    public CreateOrderResponse createOrder(CreateOrderItemsRequest request) {
        return restTemplate.postForObject(baseUrl + "/internal/orders", request, CreateOrderResponse.class);
    }

    private CreateOrderResponse createOrderFallback(CreateOrderItemsRequest request, Throwable ex) {
        throw new RuntimeException("Order service unavailable");
    }
}
