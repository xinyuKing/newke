package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import com.shixi.ecommerce.service.agent.refund.data.RefundOrderDataClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfirmRefundOrderSkillTest {
    @Test
    void shouldAskForOrderNumberWhenMissing() {
        ConfirmRefundOrderSkill skill = new ConfirmRefundOrderSkill(mock(RefundOrderDataClient.class));
        RefundContext context = new RefundContext();

        RefundSkillOutput output = skill.execute(RefundSkillRequest.builder(context).build());

        assertTrue(output.getPrompt().contains("provide order number"));
        assertTrue(output.getUpdates().isEmpty());
    }

    @Test
    void shouldPopulateRealOrderSnapshotWhenOrderExists() {
        RefundOrderDataClient client = mock(RefundOrderDataClient.class);
        ConfirmRefundOrderSkill skill = new ConfirmRefundOrderSkill(client);
        RefundContext context = new RefundContext();
        context.putSlot(RefundSlots.ORDER_NO, "O20260323001");
        OrderRefundSnapshotResponse snapshot = new OrderRefundSnapshotResponse(
                "O20260323001",
                101L,
                OrderStatus.PAID,
                new BigDecimal("299.00"),
                "SF",
                "SF123456",
                null,
                LocalDateTime.of(2026, 3, 23, 9, 30),
                List.of()
        );
        when(client.getRefundSnapshot("O20260323001")).thenReturn(Optional.of(snapshot));

        RefundSkillOutput output = skill.execute(RefundSkillRequest.builder(context).build());

        assertTrue(output.getPrompt().contains("Order verified"));
        assertEquals("101", output.getUpdates().get(RefundSlots.USER_ID));
        assertEquals("PAID", output.getUpdates().get(RefundSlots.ORDER_STATUS));
        assertEquals("299.00", output.getUpdates().get(RefundSlots.ORDER_AMOUNT));
        assertEquals("NOT_RECEIVED", output.getUpdates().get(RefundSlots.DELIVERY_STATUS));
        assertEquals("SF123456", output.getUpdates().get(RefundSlots.TRACKING_NO));
    }
}
