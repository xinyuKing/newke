package com.shixi.ecommerce.service.agent.refund.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.dto.OrderRefundStatusUpdateRequest;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        return getRefundSnapshot(orderNo, null);
    }

    public Optional<OrderRefundSnapshotResponse> getRefundSnapshot(String orderNo, Long ownerUserId) {
        return getForObject(
                ownerUserId == null
                        ? "/internal/orders/{orderNo}/refund-snapshot"
                        : "/internal/orders/{orderNo}/refund-snapshot?ownerUserId={ownerUserId}",
                OrderRefundSnapshotResponse.class,
                ownerUserId == null ? new Object[] {orderNo} : new Object[] {orderNo, ownerUserId});
    }

    public OrderRefundSnapshotResponse requireRefundSnapshot(String orderNo) {
        return requireRefundSnapshot(orderNo, null);
    }

    public OrderRefundSnapshotResponse requireRefundSnapshot(String orderNo, Long ownerUserId) {
        return requireForObject(
                ownerUserId == null
                        ? "/internal/orders/{orderNo}/refund-snapshot"
                        : "/internal/orders/{orderNo}/refund-snapshot?ownerUserId={ownerUserId}",
                OrderRefundSnapshotResponse.class,
                "Order not found",
                ownerUserId == null ? new Object[] {orderNo} : new Object[] {orderNo, ownerUserId});
    }

    public Optional<TrackingResponse> getTracking(String orderNo) {
        return getTracking(orderNo, null);
    }

    public Optional<TrackingResponse> getTracking(String orderNo, Long ownerUserId) {
        return getForObject(
                ownerUserId == null
                        ? "/internal/orders/{orderNo}/tracking"
                        : "/internal/orders/{orderNo}/tracking?ownerUserId={ownerUserId}",
                TrackingResponse.class,
                ownerUserId == null ? new Object[] {orderNo} : new Object[] {orderNo, ownerUserId});
    }

    public void updateRefundStatus(String orderNo, OrderStatus status) {
        if (baseUrl.isBlank()) {
            throw new BusinessException("Order service base URL is not configured");
        }
        OrderRefundStatusUpdateRequest request = new OrderRefundStatusUpdateRequest();
        request.setStatus(status);
        try {
            restTemplate.put(baseUrl + "/internal/orders/{orderNo}/refund-status", request, orderNo);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new BusinessException(extractBusinessMessage(ex, "Order refund status update rejected"));
            }
            logger.warn("refund order status update failed for order {}: {}", orderNo, ex.getMessage());
            throw new BusinessException("Order service unavailable");
        } catch (RestClientException ex) {
            logger.warn("refund order status update failed for order {}: {}", orderNo, ex.getMessage());
            throw new BusinessException("Order service unavailable");
        }
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

    private String extractBusinessMessage(HttpStatusCodeException ex, String fallbackMessage) {
        String message = ex.getResponseBodyAsString();
        if (message == null || message.isBlank()) {
            return fallbackMessage;
        }
        String normalized = message.trim();
        try {
            JsonNode root = OBJECT_MAPPER.readTree(normalized);
            String apiMessage = root.path("message").asText(null);
            if (apiMessage != null && !apiMessage.isBlank()) {
                return apiMessage;
            }
        } catch (Exception ignored) {
            // Fall back to the raw body for non-JSON responses.
        }
        return normalized;
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
