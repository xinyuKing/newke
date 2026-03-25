package com.shixi.ecommerce.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.dto.OrderAddressSnapshotResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class UserAddressClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public UserAddressClient(RestTemplate restTemplate, @Value("${auth.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public OrderAddressSnapshotResponse getDefaultShippingAddress(Long userId) {
        if (userId == null) {
            throw new BusinessException("UserId required");
        }
        if (baseUrl.isBlank()) {
            throw new BusinessException("Auth service unavailable");
        }
        try {
            OrderAddressSnapshotResponse response = restTemplate.getForObject(
                    baseUrl + "/internal/users/{userId}/default-address", OrderAddressSnapshotResponse.class, userId);
            if (response == null) {
                throw new BusinessException("Default shipping address required");
            }
            return response;
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new BusinessException(extractBusinessMessage(ex, "Default shipping address required"));
            }
            throw new BusinessException("Auth service unavailable");
        } catch (RestClientException ex) {
            throw new BusinessException("Auth service unavailable");
        }
    }

    private String extractBusinessMessage(HttpStatusCodeException ex, String fallbackMessage) {
        if (ex == null) {
            return fallbackMessage;
        }
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return fallbackMessage;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            String message = root.path("message").asText(null);
            if (message != null && !message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
        }
        HttpStatusCode statusCode = ex.getStatusCode();
        if (statusCode != null && statusCode.is4xxClientError()) {
            return body.trim();
        }
        return fallbackMessage;
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
