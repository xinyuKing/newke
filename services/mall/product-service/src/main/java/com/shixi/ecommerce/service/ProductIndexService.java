package com.shixi.ecommerce.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.config.SearchProperties;
import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.repository.ProductRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 商品索引服务，将商品数据写入搜索索引。
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
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.searchProperties = searchProperties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据商品 ID 写入索引（仅在 OpenSearch 模式下生效）。
     *
     * @param productId 商品 ID
     */
    public void indexProduct(Long productId) {
        if (!"opensearch".equalsIgnoreCase(searchProperties.getProvider())) {
            return;
        }
        if (productId == null) {
            return;
        }
        String baseUrl = searchProperties.getOpenSearch().getUrl();
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("OpenSearch url missing, skip index");
            return;
        }
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return;
        }
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

        String index = searchProperties.getOpenSearch().getIndex();
        String url = baseUrl + "/" + index + "/_doc/" + productId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyBasicAuth(headers);

        try {
            String payload = objectMapper.writeValueAsString(doc);
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
        } catch (Exception ex) {
            log.warn("Index product failed, productId={}", productId, ex);
        }
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
