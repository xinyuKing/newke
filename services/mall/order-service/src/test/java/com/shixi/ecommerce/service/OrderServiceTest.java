package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shixi.ecommerce.domain.Order;
import com.shixi.ecommerce.domain.OrderItem;
import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.dto.OrderAddressSnapshotResponse;
import com.shixi.ecommerce.dto.OrderLineItem;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.repository.OrderItemRepository;
import com.shixi.ecommerce.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Duration;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private DuplicateOrderGuardService duplicateOrderGuardService;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private ProductClient productClient;

    @Mock
    private UserAddressClient userAddressClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private OrderStateMachine orderStateMachine;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                inventoryClient,
                idempotencyService,
                duplicateOrderGuardService,
                eventPublisher,
                productClient,
                userAddressClient,
                redisTemplate,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                orderStateMachine);
    }

    @Test
    void createOrderByItemsSplitsOrdersByMerchant() {
        when(userAddressClient.getDefaultShippingAddress(42L)).thenReturn(defaultAddress());
        when(idempotencyService.acquire("order:cart:42:idem-1", Duration.ofMinutes(10)))
                .thenReturn(IdempotencyService.AcquireResult.acquired());
        when(productClient.getProducts(List.of(1001L, 1002L)))
                .thenReturn(List.of(
                        new ProductResponse(
                                1001L,
                                7L,
                                "Keyboard",
                                "Low latency keyboard",
                                null,
                                new BigDecimal("99.00"),
                                ProductStatus.ACTIVE),
                        new ProductResponse(
                                1002L,
                                8L,
                                "Mouse",
                                "Lightweight mouse",
                                null,
                                new BigDecimal("59.50"),
                                ProductStatus.ACTIVE)));
        when(duplicateOrderGuardService.acquire(
                        duplicateKey("order:cart", 42L, List.of("1001:7:99.00:1", "1002:8:59.50:2"), defaultAddress())))
                .thenReturn(DuplicateOrderGuardService.AcquireResult.acquired("dup-lock"));
        when(inventoryClient.deductBatch(any())).thenReturn(true);

        CreateOrderResponse response = orderService.createOrderByItems(
                42L,
                "idem-1",
                "order:cart",
                List.of(new OrderLineItem(1001L, 1, BigDecimal.ZERO), new OrderLineItem(1002L, 2, BigDecimal.ZERO)));

        ArgumentCaptor<List<Order>> ordersCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<OrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).saveAll(ordersCaptor.capture());
        verify(orderItemRepository).saveAll(orderItemsCaptor.capture());
        verify(eventPublisher, times(2)).publishOrderCreated(anyString());
        verify(idempotencyService).complete(anyString(), anyString());

        List<Order> savedOrders = ordersCaptor.getValue();
        List<OrderItem> savedItems = orderItemsCaptor.getValue();
        assertEquals(2, savedOrders.size());
        assertEquals(2, savedItems.size());
        assertTrue(response.isSplitByMerchant());
        assertEquals(2, response.getOrderNos().size());
        assertNotNull(response.getOrderNo());
        assertEquals(response.getOrderNo(), response.getOrderNos().get(0));
        assertTrue(savedOrders.stream().map(Order::getMerchantId).allMatch(id -> id == 7L || id == 8L));
        assertTrue(savedItems.stream().map(OrderItem::getMerchantId).allMatch(id -> id == 7L || id == 8L));
        assertEquals("Alice", savedOrders.get(0).getReceiverName());
        assertEquals("No. 1 Century Avenue", savedOrders.get(0).getDetailAddress());
        assertEquals("Keyboard", savedItems.get(0).getProductName());
        assertEquals("Lightweight mouse", savedItems.get(1).getProductDescription());
    }

    @Test
    void createOrderByItemsReplaysStoredResponse() throws Exception {
        CreateOrderResponse cached =
                new CreateOrderResponse("ORD-1", OrderStatus.CREATED, List.of("ORD-1", "ORD-2"), true);
        String payload = new ObjectMapper().writeValueAsString(cached);
        when(idempotencyService.acquire("order:cart:42:idem-1", Duration.ofMinutes(10)))
                .thenReturn(IdempotencyService.AcquireResult.replay(payload));

        CreateOrderResponse response = orderService.createOrderByItems(
                42L, "idem-1", "order:cart", List.of(new OrderLineItem(1001L, 1, BigDecimal.ZERO)));

        assertEquals("ORD-1", response.getOrderNo());
        assertEquals(List.of("ORD-1", "ORD-2"), response.getOrderNos());
        assertTrue(response.isSplitByMerchant());
        verifyNoInteractions(
                userAddressClient,
                productClient,
                inventoryClient,
                orderRepository,
                orderItemRepository,
                duplicateOrderGuardService,
                eventPublisher);
    }

    @Test
    void createOrderByItemsReplaysRecentDuplicateAcrossDifferentIdempotencyKeys() throws Exception {
        when(userAddressClient.getDefaultShippingAddress(42L)).thenReturn(defaultAddress());
        CreateOrderResponse cached = new CreateOrderResponse("ORD-9", OrderStatus.CREATED, List.of("ORD-9"), false);
        String payload = new ObjectMapper().writeValueAsString(cached);
        when(idempotencyService.acquire("order:cart:42:idem-2", Duration.ofMinutes(10)))
                .thenReturn(IdempotencyService.AcquireResult.acquired());
        when(productClient.getProducts(List.of(1001L)))
                .thenReturn(List.of(new ProductResponse(
                        1001L,
                        7L,
                        "Keyboard",
                        "Low latency keyboard",
                        null,
                        new BigDecimal("99.00"),
                        ProductStatus.ACTIVE)));
        when(duplicateOrderGuardService.acquire(
                        duplicateKey("order:cart", 42L, List.of("1001:7:99.00:1"), defaultAddress())))
                .thenReturn(DuplicateOrderGuardService.AcquireResult.replay(payload));

        CreateOrderResponse response = orderService.createOrderByItems(
                42L, "idem-2", "order:cart", List.of(new OrderLineItem(1001L, 1, BigDecimal.ZERO)));

        assertEquals("ORD-9", response.getOrderNo());
        verify(idempotencyService).complete("order:cart:42:idem-2", payload);
        verifyNoInteractions(inventoryClient, orderRepository, orderItemRepository, eventPublisher);
    }

    @Test
    void createOrderByItemsRequiresDefaultShippingAddress() {
        when(userAddressClient.getDefaultShippingAddress(42L))
                .thenThrow(new com.shixi.ecommerce.common.BusinessException("Default shipping address required"));
        when(idempotencyService.acquire("order:cart:42:idem-1", Duration.ofMinutes(10)))
                .thenReturn(IdempotencyService.AcquireResult.acquired());

        var exception = assertThrows(
                com.shixi.ecommerce.common.BusinessException.class,
                () -> orderService.createOrderByItems(
                        42L, "idem-1", "order:cart", List.of(new OrderLineItem(1001L, 1, BigDecimal.ZERO))));

        assertEquals("Default shipping address required", exception.getMessage());
        verify(idempotencyService).release("order:cart:42:idem-1");
        verifyNoInteractions(productClient, inventoryClient, orderRepository, orderItemRepository);
    }

    @Test
    void payOrderUpdatesOnlyOwnedOrder() {
        when(orderRepository.updateStatusIfMatchAndUser("ORD-1", 42L, OrderStatus.CREATED, OrderStatus.PAID))
                .thenReturn(1);

        orderService.payOrder(42L, "ORD-1");

        verify(orderStateMachine).assertTransition(OrderStatus.CREATED, OrderStatus.PAID, "USER");
        verify(orderRepository).updateStatusIfMatchAndUser("ORD-1", 42L, OrderStatus.CREATED, OrderStatus.PAID);
        verify(orderRepository, never()).updateStatusIfMatch(anyString(), any(), any());
        verify(eventPublisher).publishOrderPaid("ORD-1");
    }

    @Test
    void shipOrderUsesMerchantScopedUpdate() {
        when(orderRepository.updateShipInfoByMerchant(
                        "ORD-1", 7L, OrderStatus.PAID, OrderStatus.SHIPPED, "YTO", "TRACK-1"))
                .thenReturn(1);

        orderService.shipOrder(7L, "ORD-1", " YTO ", " TRACK-1 ", "MERCHANT");

        verify(orderStateMachine).assertTransition(OrderStatus.PAID, OrderStatus.SHIPPED, "MERCHANT");
        verify(orderRepository)
                .updateShipInfoByMerchant("ORD-1", 7L, OrderStatus.PAID, OrderStatus.SHIPPED, "YTO", "TRACK-1");
        verify(orderRepository, never())
                .updateShipInfo("ORD-1", OrderStatus.PAID, OrderStatus.SHIPPED, "YTO", "TRACK-1");
    }

    @Test
    void updateRefundStatusInternalMovesOrderToRefunding() {
        Order order = new Order();
        order.setOrderNo("ORD-1");
        order.setUserId(42L);
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByOrderNo("ORD-1")).thenReturn(Optional.of(order));
        when(orderRepository.updateStatusIfMatch("ORD-1", OrderStatus.COMPLETED, OrderStatus.REFUNDING))
                .thenReturn(1);

        orderService.updateRefundStatusInternal("ORD-1", OrderStatus.REFUNDING);

        verify(orderStateMachine).assertTransition(OrderStatus.COMPLETED, OrderStatus.REFUNDING, "INTERNAL");
        verify(orderRepository).updateStatusIfMatch("ORD-1", OrderStatus.COMPLETED, OrderStatus.REFUNDING);
    }

    @Test
    void getOrderDetailUsesUserScopedCacheKey() {
        Order order = new Order();
        order.setOrderNo("ORD-1");
        order.setUserId(42L);
        order.setMerchantId(7L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(new BigDecimal("19.90"));
        order.setCarrierCode("YTO");
        order.setTrackingNo("TRACK-1");
        order.setShippedAt(LocalDateTime.of(2026, 3, 23, 12, 0));
        order.setReceiverName("Alice");
        order.setReceiverPhone("13800000000");
        order.setProvince("Shanghai");
        order.setCity("Shanghai");
        order.setDistrict("Pudong");
        order.setDetailAddress("No. 1 Century Avenue");
        order.setPostalCode("200120");
        order.setAddressTag("Home");

        OrderItem item = new OrderItem();
        item.setOrderNo("ORD-1");
        item.setMerchantId(7L);
        item.setSkuId(1001L);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("9.95"));
        item.setProductName("Wireless Keyboard");
        item.setProductDescription("Low latency profile");

        when(valueOperations.get("ver:order:ORD-1")).thenReturn("7");
        when(valueOperations.get("cache:order:42:ORD-1:v7")).thenReturn(null);
        when(orderRepository.findByOrderNoAndUserId("ORD-1", 42L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderNo("ORD-1")).thenReturn(List.of(item));

        var response = orderService.getOrderDetail(42L, "ORD-1");

        verify(valueOperations).get("cache:order:42:ORD-1:v7");
        verify(orderRepository).findByOrderNoAndUserId("ORD-1", 42L);
        verify(orderRepository, never()).findByOrderNo("ORD-1");
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
        assertEquals(7L, response.getMerchantId());
        assertFalse(response.getItems().isEmpty());
        assertEquals("Alice", response.getShippingAddress().getReceiverName());
        assertEquals("No. 1 Century Avenue", response.getShippingAddress().getDetailAddress());
        assertEquals("Wireless Keyboard", response.getItems().get(0).getProductName());
        assertEquals("Low latency profile", response.getItems().get(0).getProductDescription());
    }

    @Test
    void getRefundSnapshotIncludesProductSnapshots() {
        Order order = new Order();
        order.setOrderNo("ORD-2");
        order.setUserId(42L);
        order.setMerchantId(7L);
        order.setStatus(OrderStatus.PAID);
        order.setTotalAmount(new BigDecimal("49.90"));
        order.setReceiverName("Alice");
        order.setReceiverPhone("13800000000");
        order.setProvince("Shanghai");
        order.setCity("Shanghai");
        order.setDistrict("Pudong");
        order.setDetailAddress("No. 1 Century Avenue");
        order.setPostalCode("200120");
        order.setAddressTag("Home");

        OrderItem item = new OrderItem();
        item.setOrderNo("ORD-2");
        item.setMerchantId(7L);
        item.setSkuId(2001L);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("49.90"));
        item.setProductName("Gaming Mouse");
        item.setProductDescription("Ultra light shell");

        when(orderRepository.findByOrderNo("ORD-2")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderNo("ORD-2")).thenReturn(List.of(item));

        var response = orderService.getRefundSnapshot("ORD-2", 42L);

        assertEquals(1, response.getItems().size());
        assertEquals("Alice", response.getShippingAddress().getReceiverName());
        assertEquals("Gaming Mouse", response.getItems().get(0).getProductName());
        assertEquals("Ultra light shell", response.getItems().get(0).getProductDescription());
    }

    @Test
    void createOrderByItemsFailsFastWhenInventoryCompensationFails() {
        when(userAddressClient.getDefaultShippingAddress(42L)).thenReturn(defaultAddress());
        when(idempotencyService.acquire("order:cart:42:idem-1", Duration.ofMinutes(10)))
                .thenReturn(IdempotencyService.AcquireResult.acquired());
        when(productClient.getProducts(List.of(1001L)))
                .thenReturn(List.of(new ProductResponse(
                        1001L,
                        7L,
                        "Keyboard",
                        "Low latency keyboard",
                        null,
                        new BigDecimal("99.00"),
                        ProductStatus.ACTIVE)));
        when(duplicateOrderGuardService.acquire(
                        duplicateKey("order:cart", 42L, List.of("1001:7:99.00:1"), defaultAddress())))
                .thenReturn(DuplicateOrderGuardService.AcquireResult.acquired("dup-lock"));
        when(inventoryClient.deductBatch(any())).thenReturn(true);
        when(orderRepository.saveAll(any())).thenThrow(new IllegalStateException("DB unavailable"));
        doThrow(new IllegalStateException("Inventory compensation failed"))
                .when(inventoryClient)
                .releaseBatch(any());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrderByItems(
                        42L, "idem-1", "order:cart", List.of(new OrderLineItem(1001L, 1, BigDecimal.ZERO))));

        assertEquals("Inventory compensation failed", exception.getMessage());
        verify(idempotencyService, never()).release(anyString());
    }

    @Test
    void createOrderByItemsPropagatesInventoryServiceFailureInsteadOfStockShortage() {
        when(userAddressClient.getDefaultShippingAddress(42L)).thenReturn(defaultAddress());
        when(idempotencyService.acquire("order:cart:42:idem-1", Duration.ofMinutes(10)))
                .thenReturn(IdempotencyService.AcquireResult.acquired());
        when(productClient.getProducts(List.of(1001L)))
                .thenReturn(List.of(new ProductResponse(
                        1001L,
                        7L,
                        "Keyboard",
                        "Low latency keyboard",
                        null,
                        new BigDecimal("99.00"),
                        ProductStatus.ACTIVE)));
        when(duplicateOrderGuardService.acquire(
                        duplicateKey("order:cart", 42L, List.of("1001:7:99.00:1"), defaultAddress())))
                .thenReturn(DuplicateOrderGuardService.AcquireResult.acquired("dup-lock"));
        when(inventoryClient.deductBatch(any())).thenThrow(new IllegalStateException("Inventory service unavailable"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrderByItems(
                        42L, "idem-1", "order:cart", List.of(new OrderLineItem(1001L, 1, BigDecimal.ZERO))));

        assertEquals("Inventory service unavailable", exception.getMessage());
        verify(idempotencyService).release("order:cart:42:idem-1");
    }

    private OrderAddressSnapshotResponse defaultAddress() {
        return new OrderAddressSnapshotResponse(
                "Alice", "13800000000", "Shanghai", "Shanghai", "Pudong", "No. 1 Century Avenue", "200120", "Home");
    }

    private String duplicateKey(
            String bizPrefix, Long userId, List<String> itemSegments, OrderAddressSnapshotResponse shippingAddress) {
        StringBuilder builder = new StringBuilder();
        builder.append(bizPrefix).append(':').append(userId);
        for (String itemSegment : itemSegments) {
            builder.append('|').append(itemSegment);
        }
        String encoded;
        try {
            String json = new ObjectMapper().writeValueAsString(shippingAddress);
            encoded = java.util.Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        builder.append("|addr:").append(encoded);
        return builder.toString();
    }
}
