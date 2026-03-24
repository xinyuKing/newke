package com.shixi.ecommerce.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrderClient(RestTemplate restTemplate, @Value("${order.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @CircuitBreaker(name = "orderClient", fallbackMethod = "reviewEligibilityFallback")
    @Retry(name = "orderClient")
    @Bulkhead(name = "orderClient")
    public boolean hasCompletedPurchase(Long userId, Long skuId) {
        Boolean result = restTemplate.getForObject(
                baseUrl + "/internal/orders/review-eligibility?userId={userId}&skuId={skuId}",
                Boolean.class,
                userId,
                skuId);
        return Boolean.TRUE.equals(result);
    }

    private boolean reviewEligibilityFallback(Long userId, Long skuId, Throwable ex) {
        throw new RuntimeException("Order service unavailable", ex);
    }
}
