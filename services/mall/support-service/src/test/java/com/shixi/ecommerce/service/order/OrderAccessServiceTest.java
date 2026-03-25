package com.shixi.ecommerce.service.order;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.domain.AfterSaleTicket;
import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderItemResponse;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.service.agent.refund.data.RefundOrderDataClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderAccessServiceTest {

    @Mock
    private RefundOrderDataClient refundOrderDataClient;

    private OrderAccessService orderAccessService;

    @BeforeEach
    void setUp() {
        orderAccessService = new OrderAccessService(refundOrderDataClient);
    }

    @Test
    void requireOwnedOrderRejectsForeignOrder() {
        when(refundOrderDataClient.requireRefundSnapshot("ORD-1", 42L))
                .thenReturn(snapshot("ORD-1", 99L, OrderStatus.PAID));

        assertThrows(BusinessException.class, () -> orderAccessService.requireOwnedOrder(42L, "ORD-1"));
    }

    @Test
    void requireEligibleAfterSaleOrderRejectsIneligibleStatus() {
        when(refundOrderDataClient.requireRefundSnapshot("ORD-1", 42L))
                .thenReturn(snapshot("ORD-1", 42L, OrderStatus.CREATED));

        assertThrows(BusinessException.class, () -> orderAccessService.requireEligibleAfterSaleOrder(42L, "ORD-1"));
    }

    @Test
    void requireEligibleAfterSaleOrderReturnsSnapshotForOwnedPaidOrder() {
        OrderRefundSnapshotResponse snapshot = snapshot("ORD-1", 42L, OrderStatus.PAID);
        when(refundOrderDataClient.requireRefundSnapshot("ORD-1", 42L)).thenReturn(snapshot);

        OrderRefundSnapshotResponse actual = orderAccessService.requireEligibleAfterSaleOrder(42L, "ORD-1");

        assertSame(snapshot, actual);
    }

    @Test
    void syncAfterSaleStatusSkipsNonRefundTransitionWithoutFetchingOrderSnapshot() {
        AfterSaleTicket ticket = ticket("ORD-1", 42L, 1001L, 1, AfterSaleStatus.WAIT_PROOF);

        orderAccessService.syncAfterSaleStatus(ticket, List.of(ticket));

        verify(refundOrderDataClient, never()).requireRefundSnapshot("ORD-1", 42L);
        verify(refundOrderDataClient, never()).updateRefundStatus("ORD-1", OrderStatus.REFUNDING);
        verify(refundOrderDataClient, never()).updateRefundStatus("ORD-1", OrderStatus.REFUNDED);
    }

    @Test
    void syncAfterSaleStatusSkipsPartialApprovedTicket() {
        AfterSaleTicket ticket = ticket("ORD-1", 42L, 1001L, 1, AfterSaleStatus.APPROVED);
        when(refundOrderDataClient.requireRefundSnapshot("ORD-1", 42L))
                .thenReturn(snapshot("ORD-1", 42L, OrderStatus.COMPLETED));

        orderAccessService.syncAfterSaleStatus(ticket, List.of(ticket));

        verify(refundOrderDataClient, never()).updateRefundStatus("ORD-1", OrderStatus.REFUNDING);
        verify(refundOrderDataClient, never()).updateRefundStatus("ORD-1", OrderStatus.REFUNDED);
    }

    @Test
    void syncAfterSaleStatusMovesWholeOrderToRefundingWhenApprovedCoverageIsComplete() {
        AfterSaleTicket first = ticket("ORD-1", 42L, 1001L, 1, AfterSaleStatus.APPROVED);
        AfterSaleTicket second = ticket("ORD-1", 42L, 1002L, 1, AfterSaleStatus.APPROVED);
        when(refundOrderDataClient.requireRefundSnapshot("ORD-1", 42L))
                .thenReturn(snapshot("ORD-1", 42L, OrderStatus.COMPLETED));

        orderAccessService.syncAfterSaleStatus(second, List.of(first, second));

        verify(refundOrderDataClient).updateRefundStatus("ORD-1", OrderStatus.REFUNDING);
        verify(refundOrderDataClient, never()).updateRefundStatus("ORD-1", OrderStatus.REFUNDED);
    }

    @Test
    void syncAfterSaleStatusMovesWholeOrderToRefundedAfterAllItemsRefunded() {
        AfterSaleTicket first = ticket("ORD-1", 42L, 1001L, 1, AfterSaleStatus.REFUNDED);
        AfterSaleTicket second = ticket("ORD-1", 42L, 1002L, 1, AfterSaleStatus.REFUNDED);
        when(refundOrderDataClient.requireRefundSnapshot("ORD-1", 42L))
                .thenReturn(snapshot("ORD-1", 42L, OrderStatus.COMPLETED));

        orderAccessService.syncAfterSaleStatus(second, List.of(first, second));

        verify(refundOrderDataClient).updateRefundStatus("ORD-1", OrderStatus.REFUNDING);
        verify(refundOrderDataClient).updateRefundStatus("ORD-1", OrderStatus.REFUNDED);
    }

    private OrderRefundSnapshotResponse snapshot(String orderNo, Long userId, OrderStatus status) {
        return new OrderRefundSnapshotResponse(
                orderNo,
                userId,
                7L,
                status,
                new BigDecimal("99.00"),
                "YTO",
                "TRACK-1",
                LocalDateTime.of(2026, 3, 23, 10, 0),
                LocalDateTime.of(2026, 3, 22, 10, 0),
                List.of(
                        new OrderItemResponse(1001L, 1, new BigDecimal("49.50")),
                        new OrderItemResponse(1002L, 1, new BigDecimal("49.50"))));
    }

    private AfterSaleTicket ticket(
            String orderNo, Long userId, Long skuId, Integer quantity, AfterSaleStatus afterSaleStatus) {
        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setOrderNo(orderNo);
        ticket.setUserId(userId);
        ticket.setSkuId(skuId);
        ticket.setQuantity(quantity);
        ticket.setStatus(afterSaleStatus);
        return ticket;
    }
}
