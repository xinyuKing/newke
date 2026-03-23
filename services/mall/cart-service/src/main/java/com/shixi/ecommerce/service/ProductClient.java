package com.shixi.ecommerce.service;

import com.shixi.ecommerce.dto.ProductBatchRequest;
import com.shixi.ecommerce.dto.ProductResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 商品服务客户端，提供批量查询与本地缓存。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ProductClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProductClient(RestTemplate restTemplate, @Value("${product.service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * 单个商品查询。
     *
     * @param skuId 商品 ID
     * @return 商品信息
     */
    public ProductResponse getProduct(Long skuId) {
        return restTemplate.getForObject(baseUrl + "/internal/products/" + skuId, ProductResponse.class);
    }

    /**
     * 批量查询商品信息，减少跨服务调用次数。
     *
     * @param skuIds 商品 ID 列表
     * @return 商品信息列表
     */
    @Cacheable(
            cacheNames = "productClientCache",
            key = "#skuIds",
            condition = "#skuIds != null && #skuIds.size() <= 50")
    @CircuitBreaker(name = "productClient", fallbackMethod = "getProductsFallback")
    @Retry(name = "productClient")
    @Bulkhead(name = "productClient")
    public List<ProductResponse> getProducts(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        ProductBatchRequest request = new ProductBatchRequest();
        request.setSkuIds(skuIds);
        ProductResponse[] response =
                restTemplate.postForObject(baseUrl + "/internal/products/batch", request, ProductResponse[].class);
        if (response == null) {
            return List.of();
        }
        return Arrays.asList(response);
    }

    private List<ProductResponse> getProductsFallback(List<Long> skuIds, Throwable ex) {
        throw new RuntimeException("Product service unavailable");
    }
}
