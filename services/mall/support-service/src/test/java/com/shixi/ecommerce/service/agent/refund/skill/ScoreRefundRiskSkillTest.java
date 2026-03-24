package com.shixi.ecommerce.service.agent.refund.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundDecision;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import com.shixi.ecommerce.service.agent.refund.data.RefundOrderDataClient;
import com.shixi.ecommerce.service.agent.refund.data.RefundRiskDataService;
import com.shixi.ecommerce.service.agent.refund.data.RefundRiskProfile;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ScoreRefundRiskSkillTest {
    @Test
    void shouldEscalateToHighRiskWhenOrderAlreadyHasRefundAndHistory() {
        RefundOrderDataClient orderDataClient = mock(RefundOrderDataClient.class);
        RefundRiskDataService riskDataService = mock(RefundRiskDataService.class);
        ScoreRefundRiskSkill skill = new ScoreRefundRiskSkill(orderDataClient, riskDataService);
        RefundContext context = new RefundContext();
        context.setMessage("customer mentioned fraud and chargeback before");
        context.putSlot(RefundSlots.ORDER_NO, "O20260323004");
        context.putSlot(RefundSlots.REQUESTER_USER_ID, "66");

        when(orderDataClient.getRefundSnapshot("O20260323004", 66L))
                .thenReturn(
                        Optional.of(snapshot("O20260323004", OrderStatus.REFUNDING, new BigDecimal("3999.00"), 66L)));
        when(riskDataService.load(66L, "O20260323004")).thenReturn(new RefundRiskProfile(6, 4, 3, true));

        RefundSkillOutput output =
                skill.execute(RefundSkillRequest.builder(context).build());

        assertEquals("HIGH", output.getUpdates().get(RefundSlots.RISK_LEVEL));
        assertEquals(RefundDecision.MANUAL_REVIEW, output.getDecision());
        assertTrue(output.getPrompt().contains("refund status"));
    }

    @Test
    void shouldStayLowRiskWhenNoDataSignalsAreTriggered() {
        RefundOrderDataClient orderDataClient = mock(RefundOrderDataClient.class);
        RefundRiskDataService riskDataService = mock(RefundRiskDataService.class);
        ScoreRefundRiskSkill skill = new ScoreRefundRiskSkill(orderDataClient, riskDataService);
        RefundContext context = new RefundContext();
        context.setMessage("want to return because size is not suitable");
        context.putSlot(RefundSlots.ORDER_NO, "O20260323005");
        context.putSlot(RefundSlots.REQUESTER_USER_ID, "77");

        when(orderDataClient.getRefundSnapshot("O20260323005", 77L))
                .thenReturn(Optional.of(snapshot("O20260323005", OrderStatus.PAID, new BigDecimal("59.00"), 77L)));
        when(riskDataService.load(77L, "O20260323005")).thenReturn(new RefundRiskProfile(0, 0, 0, false));

        RefundSkillOutput output =
                skill.execute(RefundSkillRequest.builder(context).build());

        assertEquals("LOW", output.getUpdates().get(RefundSlots.RISK_LEVEL));
        assertNull(output.getDecision());
        assertTrue(output.getPrompt().contains("stable refund history"));
    }

    private OrderRefundSnapshotResponse snapshot(String orderNo, OrderStatus status, BigDecimal amount, Long userId) {
        return new OrderRefundSnapshotResponse(
                orderNo,
                userId,
                status,
                amount,
                "YTO",
                "YT10086",
                null,
                LocalDateTime.of(2026, 3, 22, 12, 0),
                List.of());
    }
}
