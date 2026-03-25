package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.domain.CartItem;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.CartItemRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, productClient, redisTemplate);
    }

    @Test
    void addItemPersistsProductSnapshotsForNewCartItem() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(productClient.getProducts(List.of(1001L)))
                .thenReturn(List.of(new ProductResponse(
                        1001L,
                        7L,
                        "Keyboard",
                        "Mechanical keyboard",
                        null,
                        new BigDecimal("199.00"),
                        ProductStatus.ACTIVE)));
        when(cartItemRepository.increaseQuantity(
                        42L, 1001L, 2, new BigDecimal("199.00"), "Keyboard", "Mechanical keyboard"))
                .thenReturn(0);

        cartService.addItem(42L, 1001L, 2);

        ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(itemCaptor.capture());
        CartItem saved = itemCaptor.getValue();
        assertEquals("Keyboard", saved.getProductNameSnapshot());
        assertEquals("Mechanical keyboard", saved.getProductDescriptionSnapshot());
        assertEquals(new BigDecimal("199.00"), saved.getPriceSnapshot());
    }

    @Test
    void listItemsRefreshesSnapshotsFromActiveProduct() {
        CartItem item = new CartItem();
        item.setUserId(42L);
        item.setSkuId(1001L);
        item.setQuantity(2);
        item.setPriceSnapshot(new BigDecimal("88.00"));
        item.setProductNameSnapshot("Old keyboard");
        item.setProductDescriptionSnapshot("Old keyboard description");

        when(cartItemRepository.findByUserId(42L)).thenReturn(List.of(item));
        when(productClient.getProducts(List.of(1001L)))
                .thenReturn(List.of(new ProductResponse(
                        1001L,
                        7L,
                        "Keyboard",
                        "Mechanical keyboard",
                        null,
                        new BigDecimal("199.00"),
                        ProductStatus.ACTIVE)));

        var response = cartService.listItems(42L);

        assertEquals(1, response.size());
        assertEquals("Keyboard", response.get(0).getProductName());
        assertEquals("Mechanical keyboard", response.get(0).getProductDescription());
        assertEquals(new BigDecimal("199.00"), response.get(0).getPriceSnapshot());
        assertEquals(ProductStatus.ACTIVE, response.get(0).getProductStatus());
        assertTrue(response.get(0).getProductAvailable());
        verify(cartItemRepository).saveAll(anyList());
    }

    @Test
    void listItemsMarksInactiveProductUnavailable() {
        CartItem item = new CartItem();
        item.setUserId(42L);
        item.setSkuId(1001L);
        item.setQuantity(2);
        item.setPriceSnapshot(new BigDecimal("88.00"));
        item.setProductNameSnapshot("Keyboard");
        item.setProductDescriptionSnapshot("Mechanical keyboard");

        when(cartItemRepository.findByUserId(42L)).thenReturn(List.of(item));
        when(productClient.getProducts(List.of(1001L)))
                .thenReturn(List.of(new ProductResponse(
                        1001L,
                        7L,
                        "Keyboard",
                        "Mechanical keyboard",
                        null,
                        new BigDecimal("199.00"),
                        ProductStatus.INACTIVE)));

        var response = cartService.listItems(42L);

        assertEquals(1, response.size());
        assertEquals(new BigDecimal("88.00"), response.get(0).getPriceSnapshot());
        assertEquals(ProductStatus.INACTIVE, response.get(0).getProductStatus());
        assertFalse(response.get(0).getProductAvailable());
        verify(cartItemRepository, never()).saveAll(anyList());
    }

    @Test
    void listItemsFallsBackToSnapshotsWhenProductServiceUnavailable() {
        CartItem item = new CartItem();
        item.setUserId(42L);
        item.setSkuId(1001L);
        item.setQuantity(2);
        item.setPriceSnapshot(new BigDecimal("88.00"));
        item.setProductNameSnapshot("Keyboard");
        item.setProductDescriptionSnapshot("Mechanical keyboard");

        when(cartItemRepository.findByUserId(42L)).thenReturn(List.of(item));
        when(productClient.getProducts(List.of(1001L))).thenThrow(new RuntimeException("Product service unavailable"));

        var response = cartService.listItems(42L);

        assertEquals(1, response.size());
        assertEquals("Keyboard", response.get(0).getProductName());
        assertEquals("Mechanical keyboard", response.get(0).getProductDescription());
        assertEquals(new BigDecimal("88.00"), response.get(0).getPriceSnapshot());
        assertNull(response.get(0).getProductStatus());
        assertNull(response.get(0).getProductAvailable());
        verify(cartItemRepository, never()).saveAll(anyList());
    }
}
