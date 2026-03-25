package com.shixi.ecommerce.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shixi.ecommerce.domain.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CacheDtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void productResponseCanRoundTripForRedisCache() throws Exception {
        ProductResponse source = new ProductResponse(
                1L, 2L, "Keyboard", "Mechanical keyboard", null, new BigDecimal("399.00"), ProductStatus.ACTIVE);

        ProductResponse restored =
                objectMapper.readValue(objectMapper.writeValueAsString(source), ProductResponse.class);

        assertEquals(source.getId(), restored.getId());
        assertEquals(source.getMerchantId(), restored.getMerchantId());
        assertEquals(source.getPrice(), restored.getPrice());
        assertEquals(source.getStatus(), restored.getStatus());
    }

    @Test
    void cartAndCursorResponsesCanRoundTripForRedisCache() throws Exception {
        CursorPageResponse<ReviewResponse> source = new CursorPageResponse<>(
                List.of(new ReviewResponse(11L, 1001L, 42L, 5, "Great", LocalDateTime.of(2026, 3, 23, 12, 0))),
                true,
                LocalDateTime.of(2026, 3, 23, 13, 0),
                11L);

        CursorPageResponse<ReviewResponse> restored = objectMapper.readValue(
                objectMapper.writeValueAsString(source), new TypeReference<CursorPageResponse<ReviewResponse>>() {});
        CartItemResponse cartItem = objectMapper.readValue(
                objectMapper.writeValueAsString(new CartItemResponse(
                        1001L,
                        2,
                        new BigDecimal("88.00"),
                        "Keyboard",
                        "Mechanical keyboard",
                        ProductStatus.ACTIVE,
                        true)),
                CartItemResponse.class);
        OrderAddressSnapshotResponse shippingAddress = objectMapper.readValue(
                objectMapper.writeValueAsString(new OrderAddressSnapshotResponse(
                        "Alice",
                        "13800000000",
                        "Shanghai",
                        "Shanghai",
                        "Pudong",
                        "No. 1 Century Avenue",
                        "200120",
                        "Home")),
                OrderAddressSnapshotResponse.class);

        assertEquals(1, restored.getItems().size());
        assertTrue(restored.isHasNext());
        assertEquals(11L, restored.getNextCursorId());
        assertEquals(1001L, cartItem.getSkuId());
        assertEquals(2, cartItem.getQuantity());
        assertEquals("Keyboard", cartItem.getProductName());
        assertEquals("Mechanical keyboard", cartItem.getProductDescription());
        assertEquals(ProductStatus.ACTIVE, cartItem.getProductStatus());
        assertTrue(cartItem.getProductAvailable());
        assertEquals("Alice", shippingAddress.getReceiverName());
        assertEquals("No. 1 Century Avenue", shippingAddress.getDetailAddress());
    }
}
