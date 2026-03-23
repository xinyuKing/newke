package com.shixi.ecommerce.service.agent.refund.data;

import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.dto.TrackingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

@Service
public class RefundOrderDataClient {
    private static final Logger logger = LoggerFactory.getLogger(RefundOrderDataClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RefundOrderDataClient(RestTemplateBuilder restTemplateBuilder,
                                 @Value("${order.service.url:http://localhost:18084}") String baseUrl,
                                 @Value("${refund.order-data.timeout-ms:3000}") long timeoutMs) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(Math.max(500L, timeoutMs)))
                .setReadTimeout(Duration.ofMillis(Math.max(500L, timeoutMs)))
                .build();
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public Optional<OrderRefundSnapshotResponse> getRefundSnapshot(String orderNo) {
        return getForObject("/internal/orders/{orderNo}/refund-snapshot", OrderRefundSnapshotResponse.class, orderNo);
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
