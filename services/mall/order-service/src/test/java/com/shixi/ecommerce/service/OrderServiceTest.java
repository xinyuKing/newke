package com.shixi.ecommerce.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shixi.ecommerce.domain.Order;
import com.shixi.ecommerce.domain.OrderItem;
import com.shixi.ecommerce.domain.OrderStatus;
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
    private OrderEventPublisher eventPublisher;

    @Mock
    private ProductClient productClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private OrderStateMachine orderStateMachine;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                inventoryClient,
                idempotencyService,
                eventPublisher,
                productClient,
                redisTemplate,
                new ObjectMapper().registerModule(new JavaTimeModule()),
                orderStateMachine);
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
    void getOrderDetailUsesUserScopedCacheKey() {
        Order order = new Order();
        order.setOrderNo("ORD-1");
        order.setUserId(42L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(new BigDecimal("19.90"));
        order.setCarrierCode("YTO");
        order.setTrackingNo("TRACK-1");
        order.setShippedAt(LocalDateTime.of(2026, 3, 23, 12, 0));

        OrderItem item = new OrderItem();
        item.setOrderNo("ORD-1");
        item.setSkuId(1001L);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("9.95"));

        when(valueOperations.get("ver:order:ORD-1")).thenReturn("7");
        when(valueOperations.get("cache:order:42:ORD-1:v7")).thenReturn(null);
        when(orderRepository.findByOrderNoAndUserId("ORD-1", 42L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderNo("ORD-1")).thenReturn(List.of(item));

        orderService.getOrderDetail(42L, "ORD-1");

        verify(valueOperations).get("cache:order:42:ORD-1:v7");
        verify(orderRepository).findByOrderNoAndUserId("ORD-1", 42L);
        verify(orderRepository, never()).findByOrderNo("ORD-1");
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }
}
