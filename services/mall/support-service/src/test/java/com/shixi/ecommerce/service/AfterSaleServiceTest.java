package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.domain.AfterSaleTicket;
import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.AfterSaleCreateRequest;
import com.shixi.ecommerce.dto.AfterSaleResponse;
import com.shixi.ecommerce.dto.OrderItemResponse;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.repository.AfterSaleTicketRepository;
import com.shixi.ecommerce.service.order.OrderAccessService;
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
class AfterSaleServiceTest {

    @Mock
    private AfterSaleTicketRepository repository;

    @Mock
    private OrderAccessService orderAccessService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AfterSaleService afterSaleService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient()
                .when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        afterSaleService =
                new AfterSaleService(repository, orderAccessService, new AfterSaleStateMachine(), redisTemplate);
    }

    @Test
    void createRejectsOverlappingAfterSaleTicket() {
        AfterSaleCreateRequest request = request("ORD-1", 1001L, 1, "Damaged");
        AfterSaleTicket existing = new AfterSaleTicket();
        existing.setOrderNo("ORD-1");
        existing.setSkuId(1001L);
        existing.setQuantity(2);
        existing.setStatus(AfterSaleStatus.REFUNDED);
        when(orderAccessService.requireEligibleAfterSaleOrder(42L, "ORD-1")).thenReturn(snapshot());
        when(repository.findAllByOrderNo("ORD-1")).thenReturn(List.of(existing));

        assertThrows(BusinessException.class, () -> afterSaleService.create(42L, request));
        verify(orderAccessService).requireEligibleAfterSaleOrder(42L, "ORD-1");
    }

    @Test
    void createPersistsPerSkuAfterSaleWithDefaultQuantity() {
        AfterSaleCreateRequest request = request("ORD-1", 1001L, null, "Damaged");
        when(orderAccessService.requireEligibleAfterSaleOrder(42L, "ORD-1")).thenReturn(snapshot());
        when(repository.findAllByOrderNo("ORD-1")).thenReturn(List.of());
        when(repository.save(any(AfterSaleTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AfterSaleResponse response = afterSaleService.create(42L, request);

        ArgumentCaptor<AfterSaleTicket> ticketCaptor = ArgumentCaptor.forClass(AfterSaleTicket.class);
        verify(repository).save(ticketCaptor.capture());
        AfterSaleTicket savedTicket = ticketCaptor.getValue();
        assertEquals(42L, savedTicket.getUserId());
        assertEquals("ORD-1", savedTicket.getOrderNo());
        assertEquals(1001L, savedTicket.getSkuId());
        assertEquals(2, savedTicket.getQuantity());
        assertEquals("Damaged", savedTicket.getReason());
        assertEquals(AfterSaleStatus.INIT, savedTicket.getStatus());
        assertEquals(1001L, response.getSkuId());
        assertEquals(2, response.getQuantity());
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setStatus(AfterSaleStatus.INIT);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThrows(BusinessException.class, () -> afterSaleService.updateStatus(1L, AfterSaleStatus.REFUNDED));
        verify(repository, never()).save(any(AfterSaleTicket.class));
    }

    @Test
    void createAllowsRemainingQuantityForSameSku() {
        AfterSaleCreateRequest request = request("ORD-1", 1001L, null, "Need second claim");
        AfterSaleTicket existing = new AfterSaleTicket();
        existing.setOrderNo("ORD-1");
        existing.setSkuId(1001L);
        existing.setQuantity(1);
        existing.setStatus(AfterSaleStatus.REFUNDED);
        when(orderAccessService.requireEligibleAfterSaleOrder(42L, "ORD-1")).thenReturn(snapshot());
        when(repository.findAllByOrderNo("ORD-1")).thenReturn(List.of(existing));
        when(repository.save(any(AfterSaleTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AfterSaleResponse response = afterSaleService.create(42L, request);

        assertEquals(1, response.getQuantity());
    }

    @Test
    void updateStatusApprovedSyncsOrderRefundingStatus() {
        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setOrderNo("ORD-1");
        ticket.setStatus(AfterSaleStatus.REVIEWING);
        when(repository.findById(1L)).thenReturn(Optional.of(ticket));
        when(repository.saveAndFlush(any(AfterSaleTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        afterSaleService.updateStatus(1L, AfterSaleStatus.APPROVED);

        verify(orderAccessService).syncAfterSaleStatus("ORD-1", AfterSaleStatus.APPROVED);
    }

    @Test
    void createRejectsConcurrentRequestForSameOrder() {
        AfterSaleCreateRequest request = request("ORD-1", 1001L, 1, "Damaged");
        when(valueOperations.setIfAbsent(eq("lock:after-sale:create:ORD-1"), anyString(), any(Duration.class)))
                .thenReturn(false);

        assertThrows(BusinessException.class, () -> afterSaleService.create(42L, request));
        verify(orderAccessService, never()).requireEligibleAfterSaleOrder(42L, "ORD-1");
    }

    private AfterSaleCreateRequest request(String orderNo, Long skuId, Integer quantity, String reason) {
        AfterSaleCreateRequest request = new AfterSaleCreateRequest();
        request.setOrderNo(orderNo);
        request.setSkuId(skuId);
        request.setQuantity(quantity);
        request.setReason(reason);
        return request;
    }

    private OrderRefundSnapshotResponse snapshot() {
        return new OrderRefundSnapshotResponse(
                "ORD-1",
                42L,
                7L,
                OrderStatus.PAID,
                new BigDecimal("99.00"),
                "YTO",
                "TRACK-1",
                LocalDateTime.of(2026, 3, 23, 10, 0),
                LocalDateTime.of(2026, 3, 22, 10, 0),
                List.of(new OrderItemResponse(1001L, 2, new BigDecimal("49.50"))));
    }
}
