package com.shixi.ecommerce.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.CursorPageResponse;
import com.shixi.ecommerce.dto.ProductCreateRequest;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.ProductRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品领域服务，包含商品创建、查询与缓存策略。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ProductService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final Duration PRODUCT_CACHE_TTL = Duration.ofMinutes(5);
    private static final String PRODUCT_VERSION_PREFIX = "ver:product:";
    private static final String PRODUCT_CACHE_PREFIX = "cache:product:";
    private static final String SEARCH_VERSION_KEY = "ver:search:products";

    private final ProductRepository productRepository;
    private final InventoryClient inventoryClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductMapper productMapper;
    private final ProductIndexPublisher productIndexPublisher;

    public ProductService(
            ProductRepository productRepository,
            InventoryClient inventoryClient,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            ProductMapper productMapper,
            ProductIndexPublisher productIndexPublisher) {
        this.productRepository = productRepository;
        this.inventoryClient = inventoryClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.productMapper = productMapper;
        this.productIndexPublisher = productIndexPublisher;
    }

    /**
     * 创建商品并初始化库存，同时刷新相关缓存。
     *
     * @param merchantId 商家 ID
     * @param request    商品创建请求
     * @return 商品响应
     */
    @Transactional
    @Caching(
            evict = {
                @CacheEvict(cacheNames = "activeProducts", allEntries = true),
                @CacheEvict(cacheNames = "merchantProducts", allEntries = true),
                @CacheEvict(cacheNames = "merchantAnalysis", key = "#merchantId")
            },
            put = {@CachePut(cacheNames = "productById", key = "#result.id")})
    public ProductResponse createProduct(Long merchantId, ProductCreateRequest request) {
        Product product = new Product();
        product.setMerchantId(merchantId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setVideoUrl(request.getVideoUrl());
        product.setPrice(request.getPrice());
        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);
        inventoryClient.initStock(product.getId(), request.getStock());
        bumpProductVersion(product.getId());
        bumpSearchVersion();
        productIndexPublisher.publishAfterCommit(product.getId());
        return productMapper.toResponse(product);
    }

    @Cacheable(cacheNames = "activeProducts", key = "'all'", condition = "#page == null && #size == null")
    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts(Integer page, Integer size) {
        if (page == null && size == null) {
            return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                    .map(productMapper::toResponse)
                    .collect(Collectors.toList());
        }
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        return productRepository
                .findByStatus(
                        ProductStatus.ACTIVE, PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "id")))
                .getContent()
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 游标分页查询上架商品。
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductResponse> listActiveProductsCursor(
            LocalDateTime cursorTime, Long cursorId, Integer size) {
        int pageSize = normalizeSize(size);
        List<Product> products = productRepository.findByStatusCursor(
                ProductStatus.ACTIVE,
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

    @Cacheable(cacheNames = "merchantProducts", key = "#merchantId", condition = "#page == null && #size == null")
    @Transactional(readOnly = true)
    public List<ProductResponse> listByMerchant(Long merchantId, Integer page, Integer size) {
        if (page == null && size == null) {
            return productRepository.findByMerchantId(merchantId).stream()
                    .map(productMapper::toResponse)
                    .collect(Collectors.toList());
        }
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        return productRepository
                .findByMerchantId(merchantId, PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "id")))
                .getContent()
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 游标分页查询商家商品。
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductResponse> listByMerchantCursor(
            Long merchantId, LocalDateTime cursorTime, Long cursorId, Integer size) {
        int pageSize = normalizeSize(size);
        List<Product> products = productRepository.findByMerchantCursor(
                merchantId,
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

    @Transactional(readOnly = true)
    public Product getProductOrThrow(Long skuId) {
        return productRepository
                .findById(skuId)
                .orElseThrow(() -> new BusinessException("Product not found: " + skuId));
    }

    @Transactional(readOnly = true)
    public Product getActiveProductOrThrow(Long skuId) {
        Product product = getProductOrThrow(skuId);
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException("Product inactive: " + skuId);
        }
        return product;
    }

    /**
     * 查询商品详情（带版本号缓存）。
     *
     * @param skuId 商品 ID
     * @return 商品详情
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductResponse(Long skuId) {
        String version = getVersion(productVersionKey(skuId));
        String cacheKey = PRODUCT_CACHE_PREFIX + skuId + ":v" + version;
        ProductResponse cached = getCache(cacheKey, new TypeReference<ProductResponse>() {});
        if (cached != null) {
            return cached;
        }
        ProductResponse response = productMapper.toResponse(getProductOrThrow(skuId));
        setCache(cacheKey, response, PRODUCT_CACHE_TTL);
        return response;
    }

    /**
     * 批量查询商品信息，按输入顺序返回。
     *
     * @param skuIds 商品 ID 列表
     * @return 商品响应列表
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductResponses(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return List.of();
        }
        List<Long> uniqueSkuIds = new ArrayList<>(new LinkedHashSet<>(skuIds));
        Map<Long, String> versions = getVersions(uniqueSkuIds);
        Map<Long, ProductResponse> responseMap = getBatchCache(uniqueSkuIds, versions);
        if (responseMap.size() < uniqueSkuIds.size()) {
            List<Long> missedIds = uniqueSkuIds.stream()
                    .filter(id -> !responseMap.containsKey(id))
                    .toList();
            List<Product> products = productRepository.findAllById(missedIds);
            Map<String, String> cacheEntries = new LinkedHashMap<>();
            for (Product product : products) {
                ProductResponse response = productMapper.toResponse(product);
                responseMap.put(product.getId(), response);
                String json = writeJson(response);
                if (json != null) {
                    cacheEntries.put(
                            productCacheKey(product.getId(), versions.getOrDefault(product.getId(), "0")), json);
                }
            }
            setBatchCache(cacheEntries, PRODUCT_CACHE_TTL);
        }
        List<ProductResponse> responses = new ArrayList<>(uniqueSkuIds.size());
        for (Long id : uniqueSkuIds) {
            ProductResponse response = responseMap.get(id);
            if (response != null) {
                responses.add(response);
            }
        }
        return responses;
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String productVersionKey(Long skuId) {
        return PRODUCT_VERSION_PREFIX + skuId;
    }

    private String productCacheKey(Long skuId, String version) {
        return PRODUCT_CACHE_PREFIX + skuId + ":v" + version;
    }

    private void bumpProductVersion(Long skuId) {
        if (skuId == null) {
            return;
        }
        redisTemplate.opsForValue().increment(productVersionKey(skuId));
    }

    private void bumpSearchVersion() {
        redisTemplate.opsForValue().increment(SEARCH_VERSION_KEY);
    }

    private String getVersion(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return "0";
        }
        return value;
    }

    private Map<Long, String> getVersions(List<Long> skuIds) {
        List<String> versionKeys = skuIds.stream().map(this::productVersionKey).collect(Collectors.toList());
        List<String> versionValues;
        try {
            versionValues = redisTemplate.opsForValue().multiGet(versionKeys);
        } catch (Exception ex) {
            versionValues = null;
        }
        Map<Long, String> versions = new HashMap<>(skuIds.size() * 2);
        for (int i = 0; i < skuIds.size(); i++) {
            String version = versionValues != null && i < versionValues.size() ? versionValues.get(i) : null;
            versions.put(skuIds.get(i), version == null ? "0" : version);
        }
        return versions;
    }

    private Map<Long, ProductResponse> getBatchCache(List<Long> skuIds, Map<Long, String> versions) {
        List<String> cacheKeys = skuIds.stream()
                .map(id -> productCacheKey(id, versions.getOrDefault(id, "0")))
                .collect(Collectors.toList());
        List<String> cacheValues;
        try {
            cacheValues = redisTemplate.opsForValue().multiGet(cacheKeys);
        } catch (Exception ex) {
            return new HashMap<>();
        }
        Map<Long, ProductResponse> responseMap = new HashMap<>(skuIds.size() * 2);
        if (cacheValues == null) {
            return responseMap;
        }
        for (int i = 0; i < skuIds.size() && i < cacheValues.size(); i++) {
            String json = cacheValues.get(i);
            if (json == null || json.isBlank()) {
                continue;
            }
            ProductResponse response = readJson(json, new TypeReference<ProductResponse>() {});
            if (response != null) {
                responseMap.put(skuIds.get(i), response);
            }
        }
        return responseMap;
    }

    private <T> T getCache(String key, TypeReference<T> typeRef) {
        String json = redisTemplate.opsForValue().get(key);
        return readJson(json, typeRef);
    }

    private void setCache(String key, Object value, Duration ttl) {
        String json = writeJson(value);
        if (json == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception ignored) {
        }
    }

    private void setBatchCache(Map<String, String> entries, Duration ttl) {
        if (entries.isEmpty()) {
            return;
        }
        try {
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) {
                    ValueOperations<String, String> ops = operations.opsForValue();
                    for (Map.Entry<String, String> entry : entries.entrySet()) {
                        ops.set(entry.getKey(), entry.getValue(), ttl);
                    }
                    return null;
                }
            });
        } catch (Exception ignored) {
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private <T> T readJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception ex) {
            return null;
        }
    }
}
