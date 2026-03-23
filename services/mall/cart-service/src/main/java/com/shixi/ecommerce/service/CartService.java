package com.shixi.ecommerce.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.CartItem;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.CartItemResponse;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.CartItemRepository;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 购物车领域服务，负责购物车增删改查。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class CartService {
    private static final Duration CART_CACHE_TTL = Duration.ofMinutes(2);
    private static final String CART_VERSION_PREFIX = "ver:cart:";
    private static final String CART_CACHE_PREFIX = "cache:cart:";

    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CartService(
            CartItemRepository cartItemRepository,
            ProductClient productClient,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.cartItemRepository = cartItemRepository;
        this.productClient = productClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 添加购物车条目，校验商品状态并保存价格快照。
     *
     * @param userId   用户 ID
     * @param skuId    商品 ID
     * @param quantity 数量
     */
    @Transactional
    public void addItem(Long userId, Long skuId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Invalid quantity");
        }
        ProductResponse product = productClient.getProducts(List.of(skuId)).stream()
                .filter(p -> p.getId().equals(skuId))
                .findFirst()
                .orElse(null);
        if (product == null || product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException("Product inactive: " + skuId);
        }
        int updated = cartItemRepository.increaseQuantity(userId, skuId, quantity, product.getPrice());
        if (updated > 0) {
            bumpCartVersion(userId);
            return;
        }
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setPriceSnapshot(product.getPrice());
        try {
            cartItemRepository.save(item);
        } catch (DataIntegrityViolationException ex) {
            int retry = cartItemRepository.increaseQuantity(userId, skuId, quantity, product.getPrice());
            if (retry == 0) {
                throw new BusinessException("Cart update failed");
            }
        }
        bumpCartVersion(userId);
    }

    /**
     * 更新购物车条目数量。
     *
     * @param userId   用户 ID
     * @param skuId    商品 ID
     * @param quantity 数量
     */
    @Transactional
    public void updateItem(Long userId, Long skuId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("Invalid quantity");
        }
        int updated = cartItemRepository.updateQuantity(userId, skuId, quantity);
        if (updated == 0) {
            throw new BusinessException("Cart item not found");
        }
        bumpCartVersion(userId);
    }

    /**
     * 查询购物车条目（带缓存）。
     *
     * @param userId 用户 ID
     * @return 购物车条目列表
     */
    @Transactional(readOnly = true)
    public List<CartItemResponse> listItems(Long userId) {
        String version = getVersion(cartVersionKey(userId));
        String cacheKey = CART_CACHE_PREFIX + userId + ":v" + version;
        List<CartItemResponse> cached = getCache(cacheKey, new TypeReference<List<CartItemResponse>>() {});
        if (cached != null) {
            return cached;
        }
        List<CartItemResponse> items = cartItemRepository.findByUserId(userId).stream()
                .map(item -> new CartItemResponse(item.getSkuId(), item.getQuantity(), item.getPriceSnapshot()))
                .collect(Collectors.toList());
        setCache(cacheKey, items, CART_CACHE_TTL);
        return items;
    }

    /**
     * 删除购物车条目。
     *
     * @param userId 用户 ID
     * @param skuId  商品 ID
     */
    @Transactional
    public void removeItem(Long userId, Long skuId) {
        cartItemRepository.deleteByUserIdAndSkuId(userId, skuId);
        bumpCartVersion(userId);
    }

    /**
     * 清空购物车。
     *
     * @param userId 用户 ID
     */
    @Transactional
    public void clear(Long userId) {
        cartItemRepository.deleteByUserId(userId);
        bumpCartVersion(userId);
    }

    /**
     * 查询购物车实体列表，供结算流程使用。
     *
     * @param userId 用户 ID
     * @return 购物车实体列表
     */
    @Transactional(readOnly = true)
    public List<CartItem> listEntityItems(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    private String cartVersionKey(Long userId) {
        return CART_VERSION_PREFIX + userId;
    }

    private String getVersion(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return "0";
        }
        return value;
    }

    private void bumpCartVersion(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.opsForValue().increment(cartVersionKey(userId));
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
