package com.shixi.ecommerce.service.agent.refund.data;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.dto.TrackingResponse;
import com.shixi.ecommerce.internal.InternalAuthRestTemplateInterceptor;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class RefundOrderDataClient {
    private static final Logger logger = LoggerFactory.getLogger(RefundOrderDataClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RefundOrderDataClient(
            RestTemplateBuilder restTemplateBuilder,
            InternalAuthRestTemplateInterceptor internalAuthRestTemplateInterceptor,
            @Value("${order.service.url:http://localhost:18084}") String baseUrl,
            @Value("${refund.order-data.timeout-ms:3000}") long timeoutMs) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(Math.max(500L, timeoutMs)))
                .setReadTimeout(Duration.ofMillis(Math.max(500L, timeoutMs)))
                .additionalInterceptors(internalAuthRestTemplateInterceptor)
                .build();
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public Optional<OrderRefundSnapshotResponse> getRefundSnapshot(String orderNo) {
        return getForObject("/internal/orders/{orderNo}/refund-snapshot", OrderRefundSnapshotResponse.class, orderNo);
    }

    public OrderRefundSnapshotResponse requireRefundSnapshot(String orderNo) {
        return requireForObject(
                "/internal/orders/{orderNo}/refund-snapshot",
                OrderRefundSnapshotResponse.class,
                "Order not found",
                orderNo);
    }

    public Optional<TrackingResponse> getTracking(String orderNo) {
        return getForObject("/internal/orders/{orderNo}/tracking", TrackingResponse.class, orderNo);
    }

    private <T> Optional<T> getForObject(String path, Class<T> type, Object... uriVariables) {
        if (baseUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(restTemplate.getForObject(baseUrl + path, type, uriVariables));
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return Optional.empty();
            }
            logger.warn("refund order data request failed for path {}: {}", path, ex.getMessage());
            return Optional.empty();
        } catch (RestClientException ex) {
            logger.warn("refund order data request failed for path {}: {}", path, ex.getMessage());
            return Optional.empty();
        }
    }

    private <T> T requireForObject(String path, Class<T> type, String notFoundMessage, Object... uriVariables) {
        if (baseUrl.isBlank()) {
            throw new BusinessException("Order service base URL is not configured");
        }
        try {
            T response = restTemplate.getForObject(baseUrl + path, type, uriVariables);
            if (response == null) {
                throw new BusinessException(notFoundMessage);
            }
            return response;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new BusinessException(notFoundMessage);
            }
            logger.warn("refund order data request failed for path {}: {}", path, ex.getMessage());
            throw new BusinessException("Order service unavailable");
        } catch (RestClientException ex) {
            logger.warn("refund order data request failed for path {}: {}", path, ex.getMessage());
            throw new BusinessException("Order service unavailable");
        }
    }

    private String normalizeBaseUrl(String source) {
        if (source == null) {
            return "";
        }
        String normalized = source.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
