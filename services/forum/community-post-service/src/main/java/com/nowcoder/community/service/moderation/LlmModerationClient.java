package com.nowcoder.community.service.moderation;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LlmModerationClient {
    private static final Logger logger = LoggerFactory.getLogger(LlmModerationClient.class);

    @Value("${community.moderation.llm.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${community.moderation.llm.api-key:}")
    private String apiKey;

    @Value("${community.moderation.llm.model:gpt-5-mini}")
    private String model;

    @Value("${community.moderation.llm.threshold:70}")
    private int threshold;

    @Value("${community.moderation.llm.timeout-ms:8000}")
    private long timeoutMs;

    @Value("${community.moderation.llm.max-reasons:3}")
    private int maxReasons;

    @Value("${community.moderation.llm.prompt-file:classpath:moderation_prompt.txt}")
    private Resource promptFile;

    private RestTemplate restTemplate;
    private String prompt;

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.prompt = loadPrompt();
    }

    public ModerationResult review(String scene, String title, String content, String mediaJson) {
        if (StringUtils.isBlank(apiKey)) {
            return ModerationResult.reject(Collections.singletonList("审核服务未配置"), Collections.singletonList("system_error"));
        }
        String input = buildInput(scene, title, content, mediaJson);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", prompt);
        body.put("input", input);
        body.put("max_output_tokens", 300);
        body.put("text", buildTextFormat());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(baseUrl + "/responses", entity, String.class);
        } catch (RestClientException e) {
            logger.warn("llm moderation request failed: {}", e.getMessage());
            return ModerationResult.reject(Collections.singletonList("审核服务繁忙，请稍后重试"), Collections.singletonList("system_error"));
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return ModerationResult.reject(Collections.singletonList("审核服务繁忙，请稍后重试"), Collections.singletonList("system_error"));
        }
        String outputText = extractOutputText(response.getBody());
        if (StringUtils.isBlank(outputText)) {
            return ModerationResult.reject(Collections.singletonList("审核服务繁忙，请稍后重试"), Collections.singletonList("system_error"));
        }

        JSONObject resultJson;
        try {
            resultJson = JSONObject.parseObject(outputText);
        } catch (Exception e) {
            logger.warn("invalid moderation json: {}", outputText);
            return ModerationResult.reject(Collections.singletonList("审核服务繁忙，请稍后重试"), Collections.singletonList("system_error"));
        }

        boolean pass = resultJson.getBooleanValue("pass");
        int score = resultJson.getIntValue("score");
        List<String> reasons = toStringList(resultJson.getJSONArray("reasons"));
        List<String> tags = toStringList(resultJson.getJSONArray("tags"));

        if (score >= threshold) {
            pass = false;
        }
        if (!pass) {
            return ModerationResult.reject(trimList(reasons), tags);
        }
        return ModerationResult.pass();
    }

    private Map<String, Object> buildTextFormat() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("pass", Map.of("type", "boolean"));
        props.put("score", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        props.put("reasons", Map.of("type", "array", "items", Map.of("type", "string"), "maxItems", maxReasons));
        props.put("tags", Map.of("type", "array", "items", Map.of("type", "string")));
        schema.put("properties", props);
        schema.put("required", List.of("pass", "score", "reasons", "tags"));
        schema.put("additionalProperties", false);

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "moderation_result");
        format.put("strict", true);
        format.put("schema", schema);

        return Map.of("format", format);
    }

    private String buildInput(String scene, String title, String content, String mediaJson) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("scene", scene);
        input.put("title", StringUtils.defaultString(title));
        input.put("content", StringUtils.defaultString(content));
        input.put("media", parseMedia(mediaJson));
        return JSONObject.toJSONString(input);
    }

    private List<Map<String, Object>> parseMedia(String mediaJson) {
        if (StringUtils.isBlank(mediaJson)) {
            return Collections.emptyList();
        }
        try {
            JSONArray array = JSONArray.parseArray(mediaJson);
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < array.size() && i < 8; i++) {
                JSONObject item = array.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                Map<String, Object> mapped = new HashMap<>();
                mapped.put("type", normalizeType(item.getString("type"), item.getString("url")));
                mapped.put("url", item.getString("url"));
                mapped.put("name", item.getString("name"));
                mapped.put("size", item.getLong("size"));
                list.add(mapped);
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String normalizeType(String type, String url) {
        if ("video".equalsIgnoreCase(type) || "image".equalsIgnoreCase(type)) {
            return type.toLowerCase(Locale.ROOT);
        }
        if (url != null && url.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|webm|ogg)(\\?.*)?$")) {
            return "video";
        }
        return "image";
    }

    private List<String> toStringList(JSONArray array) {
        if (array == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if (value != null) {
                list.add(value.toString());
            }
        }
        return list;
    }

    private List<String> trimList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.singletonList("内容未通过审核");
        }
        if (list.size() <= maxReasons) {
            return list;
        }
        return list.subList(0, maxReasons);
    }

    private String extractOutputText(String body) {
        try {
            JSONObject json = JSONObject.parseObject(body);
            JSONArray output = json.getJSONArray("output");
            if (output == null) {
                return null;
            }
            for (int i = 0; i < output.size(); i++) {
                JSONObject item = output.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                JSONArray content = item.getJSONArray("content");
                if (content == null) {
                    continue;
                }
                for (int j = 0; j < content.size(); j++) {
                    JSONObject part = content.getJSONObject(j);
                    if (part == null) {
                        continue;
                    }
                    if ("output_text".equals(part.getString("type"))) {
                        return part.getString("text");
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("failed to parse moderation output: {}", e.getMessage());
        }
        return null;
    }

    private String loadPrompt() {
        if (promptFile == null) {
            return defaultPrompt();
        }
        try {
            String text = StreamUtils.copyToString(promptFile.getInputStream(), StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(text)) {
                return text;
            }
        } catch (IOException e) {
            logger.warn("failed to load moderation prompt: {}", e.getMessage());
        }
        return defaultPrompt();
    }

    private String defaultPrompt() {
        return "你是内容审核员。根据平台社区规则判断是否可发布。"
                + "请严格输出JSON，不要输出多余文字。";
    }
}
