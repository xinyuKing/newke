package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.domain.CartItem;
import com.shixi.ecommerce.domain.CheckoutRecord;
import com.shixi.ecommerce.repository.CartItemRepository;
import com.shixi.ecommerce.repository.CheckoutRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CheckoutRecordServiceTest {

    @Mock
    private CheckoutRecordRepository checkoutRecordRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CheckoutRecordService checkoutRecordService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        checkoutRecordService = new CheckoutRecordService(
                checkoutRecordRepository, cartItemRepository, redisTemplate, new ObjectMapper());
    }

    @Test
    void prepareReusesStoredSnapshotInsteadOfCurrentCart() throws Exception {
        CheckoutRecord record = new CheckoutRecord();
        record.setBizKey("42:idem-1");
        record.setUserId(42L);
        record.setIdempotencyKey("idem-1");
        record.setCheckoutItemsJson(new ObjectMapper()
                .writeValueAsString(
                        List.of(new com.shixi.ecommerce.dto.OrderLineItem(1001L, 1, new BigDecimal("19.90")))));
        record.setCartCleared(false);
        when(checkoutRecordRepository.findByBizKey("42:idem-1")).thenReturn(Optional.of(record));

        CheckoutRecordService.CheckoutSnapshot snapshot = checkoutRecordService.prepare(42L, "idem-1");

        assertEquals(1, snapshot.items().size());
        assertEquals(1001L, snapshot.items().get(0).getSkuId());
        verify(cartItemRepository, never()).findByUserId(42L);
    }

    @Test
    void markCartClearedSubtractsOnlySnapshotQuantityOnce() throws Exception {
        CheckoutRecord record = new CheckoutRecord();
        record.setBizKey("42:idem-1");
        record.setUserId(42L);
        record.setIdempotencyKey("idem-1");
        record.setCheckoutItemsJson(new ObjectMapper()
                .writeValueAsString(
                        List.of(new com.shixi.ecommerce.dto.OrderLineItem(1001L, 1, new BigDecimal("19.90")))));
        record.setCartCleared(false);
        CartItem current = new CartItem();
        current.setUserId(42L);
        current.setSkuId(1001L);
        current.setQuantity(3);
        current.setPriceSnapshot(new BigDecimal("19.90"));
        when(checkoutRecordRepository.findByBizKey("42:idem-1")).thenReturn(Optional.of(record));
        when(cartItemRepository.findByUserId(42L)).thenReturn(List.of(current));

        checkoutRecordService.markCartCleared(42L, "idem-1");

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(cartItemCaptor.capture());
        assertEquals(2, cartItemCaptor.getValue().getQuantity());
        verify(valueOperations).increment("ver:cart:42");
    }

    @Test
    void markCartClearedKeepsLaterReducedQuantity() throws Exception {
        CheckoutRecord record = new CheckoutRecord();
        record.setBizKey("42:idem-1");
        record.setUserId(42L);
        record.setIdempotencyKey("idem-1");
        record.setCheckoutItemsJson(new ObjectMapper()
                .writeValueAsString(
                        List.of(new com.shixi.ecommerce.dto.OrderLineItem(1001L, 2, new BigDecimal("19.90")))));
        record.setCartCleared(false);
        setCreatedAt(record, LocalDateTime.of(2026, 3, 24, 10, 0));

        CartItem current = new CartItem();
        current.setUserId(42L);
        current.setSkuId(1001L);
        current.setQuantity(1);
        current.setPriceSnapshot(new BigDecimal("19.90"));
        setUpdatedAt(current, LocalDateTime.of(2026, 3, 24, 10, 5));

        when(checkoutRecordRepository.findByBizKey("42:idem-1")).thenReturn(Optional.of(record));
        when(cartItemRepository.findByUserId(42L)).thenReturn(List.of(current));

        checkoutRecordService.markCartCleared(42L, "idem-1");

        verify(cartItemRepository, never()).save(current);
        verify(cartItemRepository, never()).delete(current);
        verify(valueOperations, never()).increment("ver:cart:42");
    }

    private void setCreatedAt(CheckoutRecord record, LocalDateTime createdAt) {
        try {
            java.lang.reflect.Field field = CheckoutRecord.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(record, createdAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void setUpdatedAt(CartItem item, LocalDateTime updatedAt) {
        try {
            java.lang.reflect.Field field = CartItem.class.getDeclaredField("updatedAt");
            field.setAccessible(true);
            field.set(item, updatedAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
