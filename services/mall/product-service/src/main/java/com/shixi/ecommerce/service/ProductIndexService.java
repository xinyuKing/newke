package com.shixi.ecommerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.config.SearchProperties;
import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.repository.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 鍟嗗搧绱㈠紩鏈嶅姟锛氳礋璐ｇ储寮曞垵濮嬪寲銆佺┖绱㈠紩鍥炲～鍜屽崟鏉¤褰曞埛鏂般€?
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ProductIndexService {
    private static final Logger log = LoggerFactory.getLogger(ProductIndexService.class);

    private final ProductRepository productRepository;
    private final SearchProperties searchProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ProductIndexService(
            ProductRepository productRepository,
            SearchProperties searchProperties,
            @Qualifier("searchRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.searchProperties = searchProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 鏍规嵁鍟嗗搧 ID 鍒锋柊绱㈠紩銆?
     *
     * @param productId 鍟嗗搧 ID
     */
    public void indexProduct(Long productId) {
        if (!isSearchEngineEnabled() || productId == null) {
            return;
        }
        String baseUrl = searchProperties.getOpenSearch().getUrl();
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("OpenSearch url missing, skip index");
            return;
        }
        ensureIndexExists();
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return;
        }

        String index = searchProperties.getOpenSearch().getIndex();
        String url = baseUrl + "/" + index + "/_doc/" + productId;
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String payload = objectMapper.writeValueAsString(toDocument(product));
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        } catch (Exception ex) {
            log.warn("Index product failed, productId={}", productId, ex);
        }
    }

    public void initializeIndexIfNeeded() {
        if (!isSearchEngineEnabled() || !searchProperties.getOpenSearch().isInitializeOnStartup()) {
            return;
        }
        boolean created = ensureIndexExists();
        if (created || countIndexedDocuments() == 0L) {
            bootstrapIndexFromDatabase();
        }
    }

    public boolean isSearchEngineEnabled() {
        String provider = searchProperties.getProvider();
        if ("db".equalsIgnoreCase(provider)) {
            return false;
        }
        if ("opensearch".equalsIgnoreCase(provider)) {
            return StringUtils.hasText(searchProperties.getOpenSearch().getUrl());
        }
        return StringUtils.hasText(searchProperties.getOpenSearch().getUrl());
    }

    private boolean ensureIndexExists() {
        String baseUrl = searchProperties.getOpenSearch().getUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return false;
        }
        String url = baseUrl + "/" + searchProperties.getOpenSearch().getIndex();
        HttpEntity<Void> request = new HttpEntity<>(buildHeaders());
        try {
            restTemplate.exchange(url, HttpMethod.HEAD, request, String.class);
            return false;
        } catch (HttpClientErrorException.NotFound ex) {
            return createIndex(url);
        } catch (Exception ex) {
            log.warn("Check product search index failed", ex);
            return false;
        }
    }

    private boolean createIndex(String url) {
        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            String payload = objectMapper.writeValueAsString(buildIndexDefinition());
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, headers), String.class);
            log.info(
                    "Created product search index: {}",
                    searchProperties.getOpenSearch().getIndex());
            return true;
        } catch (Exception ex) {
            log.warn("Create product search index failed", ex);
            return false;
        }
    }

    private long countIndexedDocuments() {
        String baseUrl = searchProperties.getOpenSearch().getUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return 0L;
        }
        String url = baseUrl + "/" + searchProperties.getOpenSearch().getIndex() + "/_count";
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(buildHeaders()), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return 0L;
            }
            return objectMapper.readTree(response.getBody()).path("count").asLong(0L);
        } catch (HttpClientErrorException.NotFound ex) {
            return 0L;
        } catch (Exception ex) {
            log.warn("Count product search index documents failed", ex);
            return 0L;
        }
    }

    private void bootstrapIndexFromDatabase() {
        int batchSize = Math.max(50, searchProperties.getOpenSearch().getBootstrapBatchSize());
        long lastId = 0L;
        int total = 0;
        while (true) {
            List<Product> batch =
                    productRepository.findByIdGreaterThanOrderByIdAsc(lastId, PageRequest.of(0, batchSize));
            if (batch.isEmpty()) {
                refreshIndex();
                log.info("Bootstrap product search index finished, totalIndexed={}", total);
                return;
            }
            bulkIndexProducts(batch);
            total += batch.size();
            lastId = batch.get(batch.size() - 1).getId();
        }
    }

    private void bulkIndexProducts(List<Product> products) {
        if (products.isEmpty()) {
            return;
        }
        String index = searchProperties.getOpenSearch().getIndex();
        String url = searchProperties.getOpenSearch().getUrl() + "/_bulk";
        StringBuilder payload = new StringBuilder(products.size() * 256);
        for (Product product : products) {
            payload.append("{\"index\":{\"_index\":\"")
                    .append(index)
                    .append("\",\"_id\":\"")
                    .append(product.getId())
                    .append("\"}}\n");
            try {
                payload.append(objectMapper.writeValueAsString(toDocument(product)))
                        .append('\n');
            } catch (Exception ex) {
                throw new IllegalStateException("Serialize product for bulk index failed", ex);
            }
        }

        HttpHeaders headers = buildHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-ndjson"));
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload.toString(), headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Bulk index product response not successful: {}", response.getStatusCode());
            }
        } catch (Exception ex) {
            log.warn("Bulk index products failed, size={}", products.size(), ex);
        }
    }

    private void refreshIndex() {
        String baseUrl = searchProperties.getOpenSearch().getUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return;
        }
        String url = baseUrl + "/" + searchProperties.getOpenSearch().getIndex() + "/_refresh";
        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(buildHeaders()), String.class);
        } catch (Exception ex) {
            log.warn("Refresh product search index failed", ex);
        }
    }

    private Map<String, Object> toDocument(Product product) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", product.getId());
        doc.put("merchantId", product.getMerchantId());
        doc.put("name", product.getName());
        doc.put("description", product.getDescription());
        doc.put("videoUrl", product.getVideoUrl());
        doc.put("price", product.getPrice());
        doc.put(
                "status",
                product.getStatus() == null
                        ? ProductStatus.ACTIVE.name()
                        : product.getStatus().name());
        doc.put(
                "createdAt",
                product.getCreatedAt() == null ? null : product.getCreatedAt().toString());
        return doc;
    }

    private Map<String, Object> buildIndexDefinition() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("number_of_shards", 1);
        settings.put("number_of_replicas", 0);
        settings.put(
                "analysis",
                Map.of(
                        "normalizer",
                        Map.of(
                                "lowercase_normalizer",
                                Map.of("type", "custom", "filter", List.of("lowercase", "asciifolding")))));

        Map<String, Object> properties = new HashMap<>();
        properties.put("id", Map.of("type", "long"));
        properties.put("merchantId", Map.of("type", "long"));
        properties.put(
                "name",
                Map.of(
                        "type",
                        "text",
                        "fields",
                        Map.of(
                                "keyword",
                                Map.of("type", "keyword", "ignore_above", 256),
                                "sort",
                                Map.of("type", "keyword", "normalizer", "lowercase_normalizer", "ignore_above", 256))));
        properties.put("description", Map.of("type", "text"));
        properties.put("videoUrl", Map.of("type", "keyword", "ignore_above", 512));
        properties.put("price", Map.of("type", "scaled_float", "scaling_factor", 100));
        properties.put("status", Map.of("type", "keyword"));
        properties.put("createdAt", Map.of("type", "date", "format", "strict_date_optional_time||epoch_millis"));
        return Map.of("settings", settings, "mappings", Map.of("properties", properties));
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        applyBasicAuth(headers);
        return headers;
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
}
