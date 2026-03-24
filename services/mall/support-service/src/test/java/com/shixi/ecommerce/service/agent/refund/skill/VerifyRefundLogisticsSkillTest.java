package com.shixi.ecommerce.service.agent.refund.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.dto.TrackingResponse;
import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import com.shixi.ecommerce.service.agent.refund.data.RefundOrderDataClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VerifyRefundLogisticsSkillTest {
    @Test
    void shouldRouteToDirectRefundWhenOrderIsNotShipped() {
        RefundOrderDataClient client = mock(RefundOrderDataClient.class);
        VerifyRefundLogisticsSkill skill = new VerifyRefundLogisticsSkill(client);
        RefundContext context = new RefundContext();
        context.putSlot(RefundSlots.ORDER_NO, "O20260323002");
        context.putSlot(RefundSlots.REQUESTER_USER_ID, "88");
        when(client.getRefundSnapshot("O20260323002", 88L))
                .thenReturn(Optional.of(snapshot("O20260323002", OrderStatus.PAID, null)));
        when(client.getTracking("O20260323002", 88L)).thenReturn(Optional.empty());

        RefundSkillOutput output =
                skill.execute(RefundSkillRequest.builder(context).build());

        assertEquals("NOT_RECEIVED", output.getUpdates().get(RefundSlots.DELIVERY_STATUS));
        assertTrue(output.getUpdates().get(RefundSlots.LOGISTICS_ACTION).contains("not shipped yet"));
    }

    @Test
    void shouldRequireReturnTrackingWhenShipmentWasDelivered() {
        RefundOrderDataClient client = mock(RefundOrderDataClient.class);
        VerifyRefundLogisticsSkill skill = new VerifyRefundLogisticsSkill(client);
        RefundContext context = new RefundContext();
        context.putSlot(RefundSlots.ORDER_NO, "O20260323003");
        context.putSlot(RefundSlots.REQUESTER_USER_ID, "88");
        when(client.getRefundSnapshot("O20260323003", 88L))
                .thenReturn(Optional.of(snapshot("O20260323003", OrderStatus.SHIPPED, "YT9988")));
        when(client.getTracking("O20260323003", 88L))
                .thenReturn(Optional.of(new TrackingResponse("O20260323003", "YTO", "YT9988", "DELIVERED", List.of())));

        RefundSkillOutput output =
                skill.execute(RefundSkillRequest.builder(context).build());

        assertEquals("DELIVERED", output.getUpdates().get(RefundSlots.DELIVERY_STATUS));
        assertEquals("DELIVERED", output.getUpdates().get(RefundSlots.TRACKING_STATUS));
        assertTrue(output.getUpdates().get(RefundSlots.LOGISTICS_ACTION).contains("return shipment"));
    }

    private OrderRefundSnapshotResponse snapshot(String orderNo, OrderStatus status, String trackingNo) {
        return new OrderRefundSnapshotResponse(
                orderNo,
                88L,
                status,
                new BigDecimal("199.00"),
                "YTO",
                trackingNo,
                LocalDateTime.of(2026, 3, 23, 10, 0),
                LocalDateTime.of(2026, 3, 22, 18, 0),
                List.of());
    }
}
