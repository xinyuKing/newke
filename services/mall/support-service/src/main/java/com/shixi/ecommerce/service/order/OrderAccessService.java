package com.shixi.ecommerce.service.order;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.domain.AfterSaleTicket;
import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderItemResponse;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.service.agent.refund.data.RefundOrderDataClient;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrderAccessService {
    private static final Set<OrderStatus> AFTER_SALE_ALLOWED_STATUSES =
            EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED, OrderStatus.REFUNDING);
    private static final Set<AfterSaleStatus> ORDER_SYNC_TRIGGER_STATUSES =
            EnumSet.of(AfterSaleStatus.APPROVED, AfterSaleStatus.REFUNDED);

    private final RefundOrderDataClient refundOrderDataClient;

    public OrderAccessService(RefundOrderDataClient refundOrderDataClient) {
        this.refundOrderDataClient = refundOrderDataClient;
    }

    public OrderRefundSnapshotResponse requireOwnedOrder(Long userId, String orderNo) {
        OrderRefundSnapshotResponse snapshot = refundOrderDataClient.requireRefundSnapshot(orderNo, userId);
        if (!Objects.equals(snapshot.getUserId(), userId)) {
            throw new BusinessException("Order not owned by user");
        }
        return snapshot;
    }

    public OrderRefundSnapshotResponse requireEligibleAfterSaleOrder(Long userId, String orderNo) {
        OrderRefundSnapshotResponse snapshot = requireOwnedOrder(userId, orderNo);
        if (!AFTER_SALE_ALLOWED_STATUSES.contains(snapshot.getStatus())) {
            throw new BusinessException("Order not eligible for after-sale");
        }
        return snapshot;
    }

    public void syncAfterSaleStatus(AfterSaleTicket ticket, List<AfterSaleTicket> orderTickets) {
        if (ticket == null
                || ticket.getStatus() == null
                || !ORDER_SYNC_TRIGGER_STATUSES.contains(ticket.getStatus())
                || orderTickets == null
                || orderTickets.isEmpty()) {
            return;
        }
        OrderRefundSnapshotResponse snapshot = requireRefundSnapshot(ticket);
        if (isFullyCovered(snapshot, orderTickets, EnumSet.of(AfterSaleStatus.REFUNDED))) {
            moveOrderToRefunded(snapshot);
            return;
        }
        if (isFullyCovered(snapshot, orderTickets, EnumSet.of(AfterSaleStatus.APPROVED, AfterSaleStatus.REFUNDED))) {
            moveOrderToRefunding(snapshot);
        }
    }

    private OrderRefundSnapshotResponse requireRefundSnapshot(AfterSaleTicket ticket) {
        return requireOwnedOrder(ticket.getUserId(), ticket.getOrderNo());
    }

    private void moveOrderToRefunding(OrderRefundSnapshotResponse snapshot) {
        if (snapshot.getStatus() == OrderStatus.REFUNDING || snapshot.getStatus() == OrderStatus.REFUNDED) {
            return;
        }
        refundOrderDataClient.updateRefundStatus(snapshot.getOrderNo(), OrderStatus.REFUNDING);
    }

    private void moveOrderToRefunded(OrderRefundSnapshotResponse snapshot) {
        if (snapshot.getStatus() != OrderStatus.REFUNDING && snapshot.getStatus() != OrderStatus.REFUNDED) {
            refundOrderDataClient.updateRefundStatus(snapshot.getOrderNo(), OrderStatus.REFUNDING);
        }
        if (snapshot.getStatus() != OrderStatus.REFUNDED) {
            refundOrderDataClient.updateRefundStatus(snapshot.getOrderNo(), OrderStatus.REFUNDED);
        }
    }

    private boolean isFullyCovered(
            OrderRefundSnapshotResponse snapshot, List<AfterSaleTicket> orderTickets, Set<AfterSaleStatus> statuses) {
        if (snapshot == null
                || snapshot.getItems() == null
                || snapshot.getItems().isEmpty()) {
            return false;
        }
        for (AfterSaleTicket ticket : orderTickets) {
            if (ticket.getSkuId() == null && statuses.contains(ticket.getStatus())) {
                return true;
            }
        }
        Map<Long, Integer> coveredQuantities = new HashMap<>();
        for (AfterSaleTicket ticket : orderTickets) {
            if (ticket.getSkuId() == null || !statuses.contains(ticket.getStatus())) {
                continue;
            }
            coveredQuantities.merge(ticket.getSkuId(), safeQuantity(ticket.getQuantity()), Integer::sum);
        }
        for (OrderItemResponse item : snapshot.getItems()) {
            if (item == null || item.getSkuId() == null) {
                return false;
            }
            if (coveredQuantities.getOrDefault(item.getSkuId(), 0) < safeQuantity(item.getQuantity())) {
                return false;
            }
        }
        return true;
    }

    private int safeQuantity(Integer quantity) {
        return quantity == null ? 0 : Math.max(quantity, 0);
    }
}
