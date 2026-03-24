package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.dto.TrackingResponse;
import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundDeliveryStatus;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import com.shixi.ecommerce.service.agent.refund.data.RefundOrderDataClient;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class VerifyRefundLogisticsSkill extends AbstractRefundSkill<RefundSkillOutput> {
    private final RefundOrderDataClient orderDataClient;

    public VerifyRefundLogisticsSkill(RefundOrderDataClient orderDataClient) {
        this.orderDataClient = orderDataClient;
    }

    @Override
    public String getName() {
        return RefundSkillNames.VERIFY_LOGISTICS;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        String orderNo = context.getSlot(RefundSlots.ORDER_NO);
        Long ownerUserId = parseOwnerUserId(context);
        if (orderNo == null) {
            return new RefundSkillOutput("Ask customer to provide order number before logistics verification.");
        }

        Optional<OrderRefundSnapshotResponse> snapshotOptional =
                orderDataClient.getRefundSnapshot(orderNo, ownerUserId);
        if (snapshotOptional.isEmpty()) {
            return new RefundSkillOutput(
                    "Order " + orderNo + " was not found in order service. Ask customer to verify the order number.");
        }
        OrderRefundSnapshotResponse snapshot = snapshotOptional.get();
        TrackingResponse tracking =
                orderDataClient.getTracking(orderNo, ownerUserId).orElse(null);
        String deliveryStatus = resolveDeliveryStatus(context.getSlot(RefundSlots.DELIVERY_STATUS), snapshot, tracking);
        String action = resolveAction(snapshot, tracking, deliveryStatus);

        Map<String, String> updates = new HashMap<>();
        updates.put(RefundSlots.DELIVERY_STATUS, deliveryStatus);
        updates.put(RefundSlots.LOGISTICS_ACTION, action);
        if (snapshot.getCarrierCode() != null) {
            updates.put(RefundSlots.CARRIER_CODE, snapshot.getCarrierCode());
        }
        if (snapshot.getTrackingNo() != null) {
            updates.put(RefundSlots.TRACKING_NO, snapshot.getTrackingNo());
        }
        if (tracking != null && tracking.getStatus() != null) {
            updates.put(RefundSlots.TRACKING_STATUS, tracking.getStatus());
        }

        String prompt = "Logistics check: orderNo=" + orderNo
                + ", orderStatus=" + snapshot.getStatus()
                + ", deliveryStatus=" + deliveryStatus
                + ", trackingStatus=" + (tracking == null ? "UNKNOWN" : tracking.getStatus())
                + ", action=" + action;
        return new RefundSkillOutput(prompt, updates, null);
    }

    private String resolveDeliveryStatus(
            String currentStatus, OrderRefundSnapshotResponse snapshot, TrackingResponse tracking) {
        if (tracking != null && isDelivered(tracking.getStatus())) {
            return RefundDeliveryStatus.DELIVERED.name();
        }
        OrderStatus orderStatus = snapshot.getStatus();
        if (orderStatus == OrderStatus.COMPLETED
                || orderStatus == OrderStatus.REFUNDING
                || orderStatus == OrderStatus.REFUNDED) {
            return RefundDeliveryStatus.DELIVERED.name();
        }
        if (orderStatus == OrderStatus.CREATED
                || orderStatus == OrderStatus.PAID
                || orderStatus == OrderStatus.SHIPPED
                || orderStatus == OrderStatus.CANCELED) {
            return RefundDeliveryStatus.NOT_RECEIVED.name();
        }
        if (currentStatus != null && !currentStatus.isBlank()) {
            return currentStatus;
        }
        return RefundDeliveryStatus.UNKNOWN.name();
    }

    private String resolveAction(
            OrderRefundSnapshotResponse snapshot, TrackingResponse tracking, String deliveryStatus) {
        if (snapshot.getStatus() == OrderStatus.CREATED || snapshot.getStatus() == OrderStatus.PAID) {
            return "Order not shipped yet. Confirm whether the customer wants a direct refund before dispatch.";
        }
        if (RefundDeliveryStatus.DELIVERED.name().equals(deliveryStatus)) {
            return "Request return shipment tracking or pickup proof before refund approval.";
        }
        if (tracking != null) {
            return "Verify carrier tracking updates and consignee address before refund.";
        }
        if (snapshot.getTrackingNo() != null && !snapshot.getTrackingNo().isBlank()) {
            return "Tracking exists but live logistics is unavailable. Verify carrier status before refund.";
        }
        return "Ask whether the item has been delivered and whether shipment was signed.";
    }

    private boolean isDelivered(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String text = status.toLowerCase(Locale.ROOT);
        return text.contains("delivered")
                || text.contains("signed")
                || text.contains("completed")
                || text.contains("\u7b7e\u6536")
                || text.contains("\u5df2\u9001\u8fbe");
    }

    private Long parseOwnerUserId(RefundContext context) {
        String raw = context.getSlot(RefundSlots.REQUESTER_USER_ID);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
