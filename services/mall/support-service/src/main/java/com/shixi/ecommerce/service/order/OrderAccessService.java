package com.shixi.ecommerce.service.order;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.service.agent.refund.data.RefundOrderDataClient;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OrderAccessService {
    private static final Set<OrderStatus> AFTER_SALE_ALLOWED_STATUSES =
            EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED);

    private final RefundOrderDataClient refundOrderDataClient;

    public OrderAccessService(RefundOrderDataClient refundOrderDataClient) {
        this.refundOrderDataClient = refundOrderDataClient;
    }

    public OrderRefundSnapshotResponse requireOwnedOrder(Long userId, String orderNo) {
        OrderRefundSnapshotResponse snapshot = refundOrderDataClient.requireRefundSnapshot(orderNo);
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
}
