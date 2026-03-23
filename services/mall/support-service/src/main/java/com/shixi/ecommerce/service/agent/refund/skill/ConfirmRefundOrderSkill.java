package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundDeliveryStatus;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import com.shixi.ecommerce.service.agent.refund.data.RefundOrderDataClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ConfirmRefundOrderSkill extends AbstractRefundSkill<RefundSkillOutput> {
    private final RefundOrderDataClient orderDataClient;

    public ConfirmRefundOrderSkill(RefundOrderDataClient orderDataClient) {
        this.orderDataClient = orderDataClient;
    }

    @Override
    public String getName() {
        return RefundSkillNames.CONFIRM_ORDER;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        String orderNo = context.getSlot(RefundSlots.ORDER_NO);
        if (orderNo == null) {
            return new RefundSkillOutput("Ask customer to provide order number before refund handling.");
        }
        return orderDataClient.getRefundSnapshot(orderNo)
                .map(snapshot -> new RefundSkillOutput(buildPrompt(snapshot), buildUpdates(snapshot), null))
                .orElseGet(() -> new RefundSkillOutput(
                        "Order " + orderNo + " was not found in order service. Ask customer to verify the order number."));
    }

    private String buildPrompt(OrderRefundSnapshotResponse snapshot) {
        return "Order verified: orderNo=" + snapshot.getOrderNo()
                + ", status=" + snapshot.getStatus()
                + ", totalAmount=" + snapshot.getTotalAmount()
                + ". Continue refund reason and policy checks.";
    }

    private Map<String, String> buildUpdates(OrderRefundSnapshotResponse snapshot) {
        Map<String, String> updates = new HashMap<>();
        updates.put(RefundSlots.USER_ID, String.valueOf(snapshot.getUserId()));
        updates.put(RefundSlots.ORDER_STATUS, snapshot.getStatus().name());
        updates.put(RefundSlots.ORDER_AMOUNT, snapshot.getTotalAmount().toPlainString());
        if (snapshot.getCreatedAt() != null) {
            updates.put(RefundSlots.ORDER_CREATED_AT, snapshot.getCreatedAt().toString());
        }
        if (snapshot.getCarrierCode() != null) {
            updates.put(RefundSlots.CARRIER_CODE, snapshot.getCarrierCode());
        }
        if (snapshot.getTrackingNo() != null) {
            updates.put(RefundSlots.TRACKING_NO, snapshot.getTrackingNo());
        }
        String deliveryStatus = deriveDeliveryStatus(snapshot.getStatus());
        if (deliveryStatus != null) {
            updates.put(RefundSlots.DELIVERY_STATUS, deliveryStatus);
        }
        return updates;
    }

    private String deriveDeliveryStatus(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CREATED, PAID, SHIPPED, CANCELED -> RefundDeliveryStatus.NOT_RECEIVED.name();
            case COMPLETED, REFUNDING, REFUNDED -> RefundDeliveryStatus.DELIVERED.name();
        };
    }
}
