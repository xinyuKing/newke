package com.shixi.ecommerce.service.order;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.OrderStatus;
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
        when(refundOrderDataClient.requireRefundSnapshot("ORD-1")).thenReturn(snapshot("ORD-1", 99L, OrderStatus.PAID));

        assertThrows(BusinessException.class, () -> orderAccessService.requireOwnedOrder(42L, "ORD-1"));
    }

    @Test
    void requireEligibleAfterSaleOrderRejectsIneligibleStatus() {
        when(refundOrderDataClient.requireRefundSnapshot("ORD-1"))
                .thenReturn(snapshot("ORD-1", 42L, OrderStatus.CREATED));

        assertThrows(BusinessException.class, () -> orderAccessService.requireEligibleAfterSaleOrder(42L, "ORD-1"));
    }

    @Test
    void requireEligibleAfterSaleOrderReturnsSnapshotForOwnedPaidOrder() {
        OrderRefundSnapshotResponse snapshot = snapshot("ORD-1", 42L, OrderStatus.PAID);
        when(refundOrderDataClient.requireRefundSnapshot("ORD-1")).thenReturn(snapshot);

        OrderRefundSnapshotResponse actual = orderAccessService.requireEligibleAfterSaleOrder(42L, "ORD-1");

        assertSame(snapshot, actual);
    }

    private OrderRefundSnapshotResponse snapshot(String orderNo, Long userId, OrderStatus status) {
        return new OrderRefundSnapshotResponse(
                orderNo,
                userId,
                status,
                new BigDecimal("99.00"),
                "YTO",
                "TRACK-1",
                LocalDateTime.of(2026, 3, 23, 10, 0),
                LocalDateTime.of(2026, 3, 22, 10, 0),
                List.of());
    }
}
