package com.shixi.ecommerce.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.CartItem;
import com.shixi.ecommerce.domain.CheckoutRecord;
import com.shixi.ecommerce.dto.OrderLineItem;
import com.shixi.ecommerce.repository.CartItemRepository;
import com.shixi.ecommerce.repository.CheckoutRecordRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutRecordService {
    private static final String CART_VERSION_PREFIX = "ver:cart:";

    private final CheckoutRecordRepository checkoutRecordRepository;
    private final CartItemRepository cartItemRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CheckoutRecordService(
            CheckoutRecordRepository checkoutRecordRepository,
            CartItemRepository cartItemRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.checkoutRecordRepository = checkoutRecordRepository;
        this.cartItemRepository = cartItemRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CheckoutSnapshot prepare(Long userId, String idempotencyKey) {
        String bizKey = buildBizKey(userId, idempotencyKey);
        CheckoutRecord existing = checkoutRecordRepository.findByBizKey(bizKey).orElse(null);
        if (existing != null) {
            return new CheckoutSnapshot(parseItems(existing), existing.isCartCleared());
        }

        List<CartItem> items = cartItemRepository.findByUserId(userId);
        if (items.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }
        CheckoutRecord record = new CheckoutRecord();
        record.setBizKey(bizKey);
        record.setUserId(userId);
        record.setIdempotencyKey(idempotencyKey);
        record.setCheckoutItemsJson(writeItems(items));
        record.setCartCleared(false);
        try {
            checkoutRecordRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
            CheckoutRecord concurrent = checkoutRecordRepository
                    .findByBizKey(bizKey)
                    .orElseThrow(() -> new BusinessException("Checkout snapshot creation failed"));
            return new CheckoutSnapshot(parseItems(concurrent), concurrent.isCartCleared());
        }
        return new CheckoutSnapshot(parseItems(record), false);
    }

    @Transactional
    public void markCartCleared(Long userId, String idempotencyKey) {
        CheckoutRecord record = requireRecord(userId, idempotencyKey);
        if (record.isCartCleared()) {
            return;
        }
        List<OrderLineItem> snapshotItems = parseItems(record);
        Map<Long, CartItem> currentItems = new HashMap<>();
        for (CartItem item : cartItemRepository.findByUserId(userId)) {
            currentItems.put(item.getSkuId(), item);
        }
        boolean changed = false;
        for (OrderLineItem snapshotItem : snapshotItems) {
            CartItem current = currentItems.get(snapshotItem.getSkuId());
            if (current == null) {
                continue;
            }
            int remaining = current.getQuantity() - snapshotItem.getQuantity();
            if (remaining > 0) {
                current.setQuantity(remaining);
                cartItemRepository.save(current);
            } else {
                cartItemRepository.delete(current);
            }
            changed = true;
        }
        record.setCartCleared(true);
        checkoutRecordRepository.save(record);
        if (changed) {
            redisTemplate.opsForValue().increment(CART_VERSION_PREFIX + userId);
        }
    }

    @Transactional
    public void release(Long userId, String idempotencyKey) {
        checkoutRecordRepository
                .findByBizKey(buildBizKey(userId, idempotencyKey))
                .ifPresent(checkoutRecordRepository::delete);
    }

    private CheckoutRecord requireRecord(Long userId, String idempotencyKey) {
        return checkoutRecordRepository
                .findByBizKey(buildBizKey(userId, idempotencyKey))
                .orElseThrow(() -> new BusinessException("Checkout snapshot not found"));
    }

    private List<OrderLineItem> parseItems(CheckoutRecord record) {
        try {
            return objectMapper.readValue(record.getCheckoutItemsJson(), new TypeReference<List<OrderLineItem>>() {});
        } catch (Exception ex) {
            throw new BusinessException("Checkout snapshot corrupted");
        }
    }

    private String writeItems(List<CartItem> items) {
        List<OrderLineItem> snapshotItems = items.stream()
                .map(item -> new OrderLineItem(item.getSkuId(), item.getQuantity(), item.getPriceSnapshot()))
                .toList();
        try {
            return objectMapper.writeValueAsString(snapshotItems);
        } catch (Exception ex) {
            throw new BusinessException("Checkout snapshot creation failed");
        }
    }

    private String buildBizKey(Long userId, String idempotencyKey) {
        return userId + ":" + idempotencyKey;
    }

    public record CheckoutSnapshot(List<OrderLineItem> items, boolean cartCleared) {}
}
