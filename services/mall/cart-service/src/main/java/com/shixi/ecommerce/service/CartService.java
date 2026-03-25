package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.CartItem;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.CartItemResponse;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.CartItemRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private static final String CART_VERSION_PREFIX = "ver:cart:";

    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final StringRedisTemplate redisTemplate;

    public CartService(
            CartItemRepository cartItemRepository, ProductClient productClient, StringRedisTemplate redisTemplate) {
        this.cartItemRepository = cartItemRepository;
        this.productClient = productClient;
        this.redisTemplate = redisTemplate;
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
        int updated = cartItemRepository.increaseQuantity(
                userId, skuId, quantity, product.getPrice(), product.getName(), product.getDescription());
        if (updated > 0) {
            bumpCartVersion(userId);
            return;
        }
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setPriceSnapshot(product.getPrice());
        item.setProductNameSnapshot(product.getName());
        item.setProductDescriptionSnapshot(product.getDescription());
        try {
            cartItemRepository.save(item);
        } catch (DataIntegrityViolationException ex) {
            int retry = cartItemRepository.increaseQuantity(
                    userId, skuId, quantity, product.getPrice(), product.getName(), product.getDescription());
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
    @Transactional
    public List<CartItemResponse> listItems(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            return List.of();
        }
        ProductLookupResult productLookup = fetchProductMap(cartItems);
        List<CartItem> dirtyItems = new ArrayList<>();
        List<CartItemResponse> items = cartItems.stream()
                .map(item -> toCartItemResponse(
                        item, productLookup.products().get(item.getSkuId()), productLookup.degraded(), dirtyItems))
                .collect(Collectors.toList());
        if (!dirtyItems.isEmpty()) {
            cartItemRepository.saveAll(dirtyItems);
        }
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

    private void bumpCartVersion(Long userId) {
        if (userId == null) {
            return;
        }
        redisTemplate.opsForValue().increment(cartVersionKey(userId));
    }

    private ProductLookupResult fetchProductMap(List<CartItem> cartItems) {
        List<Long> skuIds = cartItems.stream()
                .map(CartItem::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (skuIds.isEmpty()) {
            return new ProductLookupResult(Map.of(), false);
        }
        try {
            Map<Long, ProductResponse> products = productClient.getProducts(skuIds).stream()
                    .filter(Objects::nonNull)
                    .filter(product -> product.getId() != null)
                    .collect(Collectors.toMap(
                            ProductResponse::getId, product -> product, (left, right) -> left, LinkedHashMap::new));
            return new ProductLookupResult(products, false);
        } catch (RuntimeException ex) {
            return new ProductLookupResult(Map.of(), true);
        }
    }

    private CartItemResponse toCartItemResponse(
            CartItem item, ProductResponse product, boolean degraded, List<CartItem> dirtyItems) {
        if (product == null) {
            return new CartItemResponse(
                    item.getSkuId(),
                    item.getQuantity(),
                    item.getPriceSnapshot(),
                    item.getProductNameSnapshot(),
                    item.getProductDescriptionSnapshot(),
                    null,
                    degraded ? null : Boolean.FALSE);
        }
        if (product.getStatus() != ProductStatus.ACTIVE) {
            return new CartItemResponse(
                    item.getSkuId(),
                    item.getQuantity(),
                    item.getPriceSnapshot(),
                    item.getProductNameSnapshot(),
                    item.getProductDescriptionSnapshot(),
                    product.getStatus(),
                    false);
        }
        if (applyLiveSnapshot(item, product)) {
            dirtyItems.add(item);
        }
        return new CartItemResponse(
                item.getSkuId(),
                item.getQuantity(),
                item.getPriceSnapshot(),
                item.getProductNameSnapshot(),
                item.getProductDescriptionSnapshot(),
                product.getStatus(),
                true);
    }

    private boolean applyLiveSnapshot(CartItem item, ProductResponse product) {
        boolean dirty = false;
        if (!samePrice(item.getPriceSnapshot(), product.getPrice())) {
            item.setPriceSnapshot(product.getPrice());
            dirty = true;
        }
        if (!Objects.equals(item.getProductNameSnapshot(), product.getName())) {
            item.setProductNameSnapshot(product.getName());
            dirty = true;
        }
        if (!Objects.equals(item.getProductDescriptionSnapshot(), product.getDescription())) {
            item.setProductDescriptionSnapshot(product.getDescription());
            dirty = true;
        }
        return dirty;
    }

    private boolean samePrice(java.math.BigDecimal left, java.math.BigDecimal right) {
        if (left == null || right == null) {
            return Objects.equals(left, right);
        }
        return left.compareTo(right) == 0;
    }

    private record ProductLookupResult(Map<Long, ProductResponse> products, boolean degraded) {}
}
