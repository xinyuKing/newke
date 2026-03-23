package com.shixi.ecommerce.service.agent.refund;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.config.RefundModelProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelClient {
    private static final Logger logger = LoggerFactory.getLogger(ModelClient.class);

    private final RefundModelProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ModelClient(RestTemplateBuilder restTemplateBuilder,
                       ObjectMapper objectMapper,
                       RefundModelProperties properties) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        RefundModelProperties.RefundModelClientProperties client = properties.getModel().getClient();
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(client.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(client.getTimeoutMs()))
                .build();
    }

    public String generate(AgentProfile profile, String prompt, List<String> docs) {
        String normalizedPrompt = prompt == null ? "" : prompt.trim();
        if (normalizedPrompt.isEmpty()) {
            return "";
        }

        RefundModelProperties.RefundModelClientProperties client = properties.getModel().getClient();
        if (!client.isEnabled() || isBlank(client.getBaseUrl()) || isBlank(client.getCompletionsPath())) {
            return buildFallbackResponse(normalizedPrompt);
        }

        try {
            return requestCompletion(profile, normalizedPrompt, docs, client);
        } catch (RuntimeException ex) {
            logger.warn("model client failed for agent {}: {}",
                    profile == null ? "unknown" : profile.getAgentType(), ex.getMessage());
            if (client.isMockFallback()) {
                return buildFallbackResponse(normalizedPrompt);
            }
            throw ex;
        }
    }

    private String requestCompletion(AgentProfile profile,
                                     String prompt,
                                     List<String> docs,
                                     RefundModelProperties.RefundModelClientProperties client) {
        String modelName = resolveModelName(profile);
        if (isBlank(modelName)) {
            return buildFallbackResponse(prompt);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt(profile, client)),
                Map.of("role", "user", "content", buildUserPrompt(prompt, docs))
        ));
        if (profile != null) {
            body.put("temperature", profile.getTemperature());
            body.put("max_tokens", profile.getMaxTokens());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!isBlank(client.getApiKey())) {
            headers.setBearerAuth(client.getApiKey().trim());
        }

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    resolveUrl(client), new HttpEntity<>(body, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("empty model response");
            }
            String content = extractContent(response.getBody());
            if (isBlank(content)) {
                throw new IllegalStateException("blank model content");
            }
            return content.trim();
        } catch (RestClientException ex) {
            throw new IllegalStateException("request failed: " + ex.getMessage(), ex);
        }
    }

    private String buildSystemPrompt(AgentProfile profile,
                                     RefundModelProperties.RefundModelClientProperties client) {
        String agentType = profile == null ? "GENERAL_AGENT" : profile.getAgentType();
        return "You are the " + agentType + " node in an e-commerce after-sale workflow. "
                + "Prompt version: " + client.getPromptVersion() + ". "
                + "Be concise, deterministic, policy-first, and do not invent unavailable order or logistics facts. "
                + "If retrieved docs are present, prefer them over guesswork. Return plain text only.";
    }

    private String buildUserPrompt(String prompt, List<String> docs) {
        List<String> lines = new ArrayList<>();
        lines.add("Task:");
        lines.add(prompt);
        if (docs != null && !docs.isEmpty()) {
            lines.add("");
            lines.add("Retrieved docs:");
            for (String doc : docs) {
                lines.add("- " + doc);
            }
        }
        return String.join("\n", lines);
    }

    private String extractContent(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode messageContent = root.path("choices").path(0).path("message").path("content");
            if (messageContent.isTextual()) {
                return messageContent.asText();
            }
            if (messageContent.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode node : messageContent) {
                    if (node.isTextual()) {
                        builder.append(node.asText());
                    } else if (node.hasNonNull("text")) {
                        builder.append(node.get("text").asText());
                    }
                }
                return builder.toString();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("invalid model response: " + ex.getMessage(), ex);
        }
        return null;
    }

    private String resolveModelName(AgentProfile profile) {
        if (profile == null) {
            return null;
        }
        if (!isBlank(profile.getFineTuneModel())) {
            return profile.getFineTuneModel().trim();
        }
        if (!isBlank(profile.getBaseModel())) {
            return profile.getBaseModel().trim();
        }
        return null;
    }

    private String resolveUrl(RefundModelProperties.RefundModelClientProperties client) {
        String baseUrl = client.getBaseUrl().trim();
        String path = client.getCompletionsPath().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + path;
    }

    private String buildFallbackResponse(String prompt) {
        return prompt;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
