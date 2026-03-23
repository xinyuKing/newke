package com.shixi.ecommerce.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.config.SearchProperties;
import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.CursorPageResponse;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 商品搜索服务，支持数据库与 OpenSearch 两种实现。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ProductSearchService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final SearchProperties searchProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    public ProductSearchService(
            ProductRepository productRepository,
            ProductMapper productMapper,
            SearchProperties searchProperties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.searchProperties = searchProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 搜索上架商品，默认按创建时间倒序。
     *
     * @param keyword    关键词
     * @param cursorTime 游标时间
     * @param cursorId   游标 ID
     * @param size       每页大小
     * @return 游标分页结果
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductResponse> searchActiveProducts(
            String keyword, LocalDateTime cursorTime, Long cursorId, Integer size) {
        int pageSize = normalizeSize(size);
        if (!"opensearch".equalsIgnoreCase(searchProperties.getProvider())) {
            return searchFromDb(keyword, cursorTime, cursorId, pageSize);
        }
        try {
            return searchFromOpenSearch(keyword, cursorTime, cursorId, pageSize);
        } catch (Exception ex) {
            log.warn("OpenSearch query failed, fallbackToDb={}", searchProperties.isFallbackToDb(), ex);
            if (searchProperties.isFallbackToDb()) {
                return searchFromDb(keyword, cursorTime, cursorId, pageSize);
            }
            throw new BusinessException("Search failed");
        }
    }

    private CursorPageResponse<ProductResponse> searchFromDb(
            String keyword, LocalDateTime cursorTime, Long cursorId, int pageSize) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<Product> products = productRepository.searchByStatusCursor(
                ProductStatus.ACTIVE,
                normalized,
                cursorTime,
                cursorId,
                PageRequest.of(
                        0,
                        pageSize + 1,
                        Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        boolean hasNext = products.size() > pageSize;
        if (hasNext) {
            products = products.subList(0, pageSize);
        }
        LocalDateTime nextTime = null;
        Long nextId = null;
        if (!products.isEmpty()) {
            Product last = products.get(products.size() - 1);
            nextTime = last.getCreatedAt();
            nextId = last.getId();
        }
        List<ProductResponse> items =
                products.stream().map(productMapper::toResponse).collect(Collectors.toList());
        return new CursorPageResponse<>(items, hasNext, nextTime, nextId);
    }

    private CursorPageResponse<ProductResponse> searchFromOpenSearch(
            String keyword, LocalDateTime cursorTime, Long cursorId, int pageSize) throws Exception {
        String baseUrl = searchProperties.getOpenSearch().getUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new BusinessException("OpenSearch url missing");
        }
        String index = searchProperties.getOpenSearch().getIndex();
        String url = baseUrl + "/" + index + "/_search";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", pageSize + 1);
        body.put(
                "_source",
                List.of("id", "merchantId", "name", "description", "videoUrl", "price", "status", "createdAt"));
        body.put("query", buildQuery(keyword));
        body.put("sort", List.of(Map.of("createdAt", Map.of("order", "desc")), Map.of("id", Map.of("order", "desc"))));
        if (cursorTime != null && cursorId != null) {
            body.put("search_after", List.of(cursorTime.toString(), cursorId));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyBasicAuth(headers);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException("OpenSearch query failed");
        }
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode hits = root.path("hits").path("hits");
        List<ProductResponse> items = new ArrayList<>();
        LocalDateTime nextTime = null;
        Long nextId = null;
        boolean hasNext = hits.isArray() && hits.size() > pageSize;
        int limit = Math.min(pageSize, hits.size());
        for (int i = 0; i < limit; i++) {
            JsonNode hit = hits.get(i);
            ProductResponse product = parseProduct(hit.path("_source"));
            if (product != null) {
                items.add(product);
            }
        }
        if (hits.isArray() && hits.size() > 0) {
            JsonNode last = hits.get(Math.min(limit - 1, hits.size() - 1));
            JsonNode sort = last.path("sort");
            if (sort.isArray() && sort.size() >= 2) {
                JsonNode timeNode = sort.get(0);
                JsonNode idNode = sort.get(1);
                if (timeNode != null) {
                    try {
                        if (timeNode.isNumber()) {
                            long millis = timeNode.asLong();
                            nextTime = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime();
                        } else {
                            nextTime = LocalDateTime.parse(timeNode.asText());
                        }
                    } catch (Exception ignored) {
                        nextTime = null;
                    }
                }
                if (idNode != null) {
                    try {
                        nextId = idNode.asLong();
                    } catch (Exception ignored) {
                        nextId = null;
                    }
                }
            }
        }
        return new CursorPageResponse<>(items, hasNext, nextTime, nextId);
    }

    private Map<String, Object> buildQuery(String keyword) {
        Map<String, Object> filter = Map.of("term", Map.of("status", ProductStatus.ACTIVE.name()));
        if (!StringUtils.hasText(keyword)) {
            return Map.of("bool", Map.of("filter", List.of(filter)));
        }
        Map<String, Object> must = new HashMap<>();
        String normalized = keyword.trim();
        must.put(
                "multi_match",
                Map.of("query", normalized, "fields", List.of("name^2", "description"), "type", "best_fields"));
        return Map.of(
                "bool",
                Map.of(
                        "must", List.of(must),
                        "filter", List.of(filter)));
    }

    private void applyBasicAuth(HttpHeaders headers) {
        String username = searchProperties.getOpenSearch().getUsername();
        String password = searchProperties.getOpenSearch().getPassword();
        if (!StringUtils.hasText(username)) {
            return;
        }
        String token = Base64.getEncoder()
                .encodeToString((username + ":" + (password == null ? "" : password)).getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + token);
    }

    private ProductResponse parseProduct(JsonNode source) {
        if (source == null || source.isMissingNode()) {
            return null;
        }
        Long id = source.path("id").asLong();
        Long merchantId = source.path("merchantId").asLong();
        String name = source.path("name").asText(null);
        String description = source.path("description").asText(null);
        String videoUrl = source.path("videoUrl").asText(null);
        String status = source.path("status").asText(ProductStatus.ACTIVE.name());
        JsonNode priceNode = source.path("price");
        return new ProductResponse(
                id,
                merchantId,
                name,
                description,
                videoUrl,
                priceNode.isNumber() ? priceNode.decimalValue() : java.math.BigDecimal.ZERO,
                ProductStatus.valueOf(status));
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
