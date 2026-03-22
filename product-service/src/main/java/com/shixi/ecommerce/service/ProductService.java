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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final ProductRepository productRepository;
    private final InventoryClient inventoryClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductMapper productMapper;
    private final ProductIndexPublisher productIndexPublisher;

    public ProductService(ProductRepository productRepository,
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
            put = {
                    @CachePut(cacheNames = "productById", key = "#result.id")
            }
    )
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
        productIndexPublisher.publishAfterCommit(product.getId());
        return productMapper.toResponse(product);
    }

    @Cacheable(cacheNames = "activeProducts", key = "'all'", condition = "#page == null && #size == null")
    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts(Integer page, Integer size) {
        if (page == null && size == null) {
            return productRepository.findByStatus(ProductStatus.ACTIVE)
                    .stream().map(productMapper::toResponse).collect(Collectors.toList());
        }
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        return productRepository.findByStatus(
                        ProductStatus.ACTIVE,
                        PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "id")))
                .getContent()
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 游标分页查询上架商品。
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductResponse> listActiveProductsCursor(LocalDateTime cursorTime,
                                                                        Long cursorId,
                                                                        Integer size) {
        int pageSize = normalizeSize(size);
        List<Product> products = productRepository.findByStatusCursor(
                ProductStatus.ACTIVE,
                cursorTime,
                cursorId,
                PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))));
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
        List<ProductResponse> items = products.stream().map(productMapper::toResponse).collect(Collectors.toList());
        return new CursorPageResponse<>(items, hasNext, nextTime, nextId);
    }

    @Cacheable(cacheNames = "merchantProducts", key = "#merchantId", condition = "#page == null && #size == null")
    @Transactional(readOnly = true)
    public List<ProductResponse> listByMerchant(Long merchantId, Integer page, Integer size) {
        if (page == null && size == null) {
            return productRepository.findByMerchantId(merchantId)
                    .stream().map(productMapper::toResponse).collect(Collectors.toList());
        }
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        return productRepository.findByMerchantId(
                        merchantId,
                        PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "id")))
                .getContent()
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 游标分页查询商家商品。
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<ProductResponse> listByMerchantCursor(Long merchantId,
                                                                    LocalDateTime cursorTime,
                                                                    Long cursorId,
                                                                    Integer size) {
        int pageSize = normalizeSize(size);
        List<Product> products = productRepository.findByMerchantCursor(
                merchantId,
                cursorTime,
                cursorId,
                PageRequest.of(0, pageSize + 1, Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))));
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
        List<ProductResponse> items = products.stream().map(productMapper::toResponse).collect(Collectors.toList());
        return new CursorPageResponse<>(items, hasNext, nextTime, nextId);
    }

    @Transactional(readOnly = true)
    public Product getProductOrThrow(Long skuId) {
        return productRepository.findById(skuId)
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
        Set<Long> unique = new LinkedHashSet<>(skuIds);
        List<Product> products = productRepository.findAllById(unique);
        Map<Long, Product> productMap = new HashMap<>(products.size() * 2);
        for (Product product : products) {
            productMap.put(product.getId(), product);
        }
        List<ProductResponse> responses = new ArrayList<>(unique.size());
        for (Long id : unique) {
            Product product = productMap.get(id);
            if (product != null) {
                responses.add(productMapper.toResponse(product));
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

    private void bumpProductVersion(Long skuId) {
        if (skuId == null) {
            return;
        }
        redisTemplate.opsForValue().increment(productVersionKey(skuId));
    }

    private String getVersion(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return "0";
        }
        return value;
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
}
