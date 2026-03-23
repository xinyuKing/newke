package com.shixi.ecommerce.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
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
 * 鍟嗗搧鎼滅储鏈嶅姟锛屾敮鎸丏B/OpenSearch 鍙岄€氶亾锛屽悓鏃朵负鐑棬鏌ヨ鎻愪緵鐭椂缂撳瓨銆?
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ProductSearchService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final String SEARCH_VERSION_KEY = "ver:search:products";
    private static final String SEARCH_CACHE_PREFIX = "cache:search:products:";

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final SearchProperties searchProperties;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    public ProductSearchService(
            ProductRepository productRepository,
            ProductMapper productMapper,
            SearchProperties searchProperties,
            StringRedisTemplate redisTemplate,
            @Qualifier("searchRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.searchProperties = searchProperties;
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 鎼滅储涓婃灦鍟嗗搧锛氫紭鍏堣蛋 OpenSearch锛屽け璐ユ椂鎸夐厤缃喅瀹氭槸鍚﹂檷绾у埌 DB銆?
     *
     * @param keyword 鍏抽敭璇?
     * @param cursorTime 娓告爣鏃堕棿
     * @param cursorId 娓告爣 ID
     * @param size 姣忛〉澶у皬
     * @return 娓告爣鍒嗛〉缁撴灉
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductResponse> searchActiveProducts(
            String keyword, LocalDateTime cursorTime, Long cursorId, Integer size) {
        int pageSize = normalizeSize(size);
        String normalizedKeyword = normalizeKeyword(keyword);
        String provider = resolveProvider();
        String version = getVersion(SEARCH_VERSION_KEY);
        String cacheKey = buildCacheKey(provider, normalizedKeyword, cursorTime, cursorId, pageSize, version);
        CursorPageResponse<ProductResponse> cached =
                getCache(cacheKey, new TypeReference<CursorPageResponse<ProductResponse>>() {});
        if (cached != null) {
            return cached;
        }

        CursorPageResponse<ProductResponse> response;
        if (!isOpenSearchProvider(provider)) {
            response = searchFromDb(normalizedKeyword, cursorTime, cursorId, pageSize);
            setCache(cacheKey, response, searchCacheTtl());
            return response;
        }

        try {
            response = searchFromOpenSearch(normalizedKeyword, cursorTime, cursorId, pageSize);
        } catch (Exception ex) {
            log.warn("OpenSearch query failed, fallbackToDb={}", searchProperties.isFallbackToDb(), ex);
            if (!searchProperties.isFallbackToDb()) {
                throw new BusinessException("Search failed");
            }
            response = searchFromDb(normalizedKeyword, cursorTime, cursorId, pageSize);
        }
        setCache(cacheKey, response, searchCacheTtl());
        return response;
    }

    private CursorPageResponse<ProductResponse> searchFromDb(
            String keyword, LocalDateTime cursorTime, Long cursorId, int pageSize) {
        List<Product> products;
        if (keyword == null) {
            products = productRepository.findByStatusCursor(
                    ProductStatus.ACTIVE,
                    cursorTime,
                    cursorId,
                    PageRequest.of(
                            0,
                            pageSize + 1,
                            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        } else {
            products = productRepository.searchByStatusCursor(
                    ProductStatus.ACTIVE,
                    keyword,
                    cursorTime,
                    cursorId,
                    PageRequest.of(
                            0,
                            pageSize + 1,
                            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        }
        return toCursorResponse(products, pageSize);
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
        body.put("track_total_hits", false);
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
        List<SearchHitProduct> products = new ArrayList<>();
        if (hits.isArray()) {
            int limit = Math.min(hits.size(), pageSize + 1);
            for (int i = 0; i < limit; i++) {
                SearchHitProduct parsed = parseProduct(hits.get(i).path("_source"));
                if (parsed != null) {
                    products.add(parsed);
                }
            }
        }
        return toCursorResponseFromSearch(products, pageSize);
    }

    private CursorPageResponse<ProductResponse> toCursorResponse(List<Product> products, int pageSize) {
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

    private CursorPageResponse<ProductResponse> toCursorResponseFromSearch(
            List<SearchHitProduct> products, int pageSize) {
        boolean hasNext = products.size() > pageSize;
        if (hasNext) {
            products = products.subList(0, pageSize);
        }
        LocalDateTime nextTime = null;
        Long nextId = null;
        if (!products.isEmpty()) {
            SearchHitProduct last = products.get(products.size() - 1);
            nextTime = last.createdAt();
            nextId = last.id();
        }
        List<ProductResponse> items =
                products.stream().map(SearchHitProduct::response).collect(Collectors.toList());
        return new CursorPageResponse<>(items, hasNext, nextTime, nextId);
    }

    private Map<String, Object> buildQuery(String keyword) {
        Map<String, Object> filter = Map.of("term", Map.of("status", ProductStatus.ACTIVE.name()));
        if (!StringUtils.hasText(keyword)) {
            return Map.of("bool", Map.of("filter", List.of(filter)));
        }
        String normalized = keyword.trim();
        List<Map<String, Object>> should = new ArrayList<>();
        should.add(Map.of("match_phrase", Map.of("name", Map.of("query", normalized, "boost", 6))));
        should.add(Map.of("match_phrase_prefix", Map.of("name", Map.of("query", normalized, "boost", 4))));
        should.add(Map.of(
                "multi_match",
                Map.of(
                        "query",
                        normalized,
                        "fields",
                        List.of("name^3", "description"),
                        "type",
                        "best_fields",
                        "operator",
                        "and",
                        "boost",
                        2)));
        should.add(Map.of(
                "multi_match",
                Map.of("query", normalized, "fields", List.of("name^2", "description"), "type", "phrase_prefix")));
        return Map.of("bool", Map.of("should", should, "minimum_should_match", 1, "filter", List.of(filter)));
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

    private SearchHitProduct parseProduct(JsonNode source) {
        if (source == null || source.isMissingNode()) {
            return null;
        }
        JsonNode priceNode = source.path("price");
        LocalDateTime createdAt = null;
        if (!source.path("createdAt").isMissingNode()
                && !source.path("createdAt").isNull()) {
            try {
                createdAt = LocalDateTime.parse(source.path("createdAt").asText());
            } catch (Exception ignored) {
                createdAt = null;
            }
        }
        ProductResponse response = new ProductResponse(
                source.path("id").asLong(),
                source.path("merchantId").asLong(),
                source.path("name").asText(null),
                source.path("description").asText(null),
                source.path("videoUrl").asText(null),
                priceNode.isNumber() ? priceNode.decimalValue() : java.math.BigDecimal.ZERO,
                ProductStatus.valueOf(source.path("status").asText(ProductStatus.ACTIVE.name())));
        return new SearchHitProduct(response, createdAt, response.getId());
    }

    private String resolveProvider() {
        String provider = searchProperties.getProvider();
        if ("db".equalsIgnoreCase(provider) || "opensearch".equalsIgnoreCase(provider)) {
            return provider.toLowerCase();
        }
        return StringUtils.hasText(searchProperties.getOpenSearch().getUrl()) ? "opensearch" : "db";
    }

    private boolean isOpenSearchProvider(String provider) {
        return "opensearch".equalsIgnoreCase(provider);
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    private Duration searchCacheTtl() {
        return Duration.ofSeconds(Math.max(1L, searchProperties.getCacheTtlSeconds()));
    }

    private String buildCacheKey(
            String provider, String keyword, LocalDateTime cursorTime, Long cursorId, int pageSize, String version) {
        String keywordKey = keyword == null
                ? "all"
                : Base64.getUrlEncoder().withoutPadding().encodeToString(keyword.getBytes(StandardCharsets.UTF_8));
        String cursorTimeKey = cursorTime == null ? "none" : cursorTime.toString();
        String cursorIdKey = cursorId == null ? "none" : String.valueOf(cursorId);
        return SEARCH_CACHE_PREFIX + provider + ":v" + version + ":" + keywordKey + ":" + cursorTimeKey + ":"
                + cursorIdKey + ":" + pageSize;
    }

    private String getVersion(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? "0" : value;
    }

    private <T> T getCache(String key, TypeReference<T> typeRef) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception ex) {
            return null;
        }
    }

    private void setCache(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception ignored) {
        }
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private record SearchHitProduct(ProductResponse response, LocalDateTime createdAt, Long id) {}
}
