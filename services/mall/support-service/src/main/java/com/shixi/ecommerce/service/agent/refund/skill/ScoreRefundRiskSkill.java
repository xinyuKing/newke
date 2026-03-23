package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundDecision;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import com.shixi.ecommerce.service.agent.refund.data.RefundOrderDataClient;
import com.shixi.ecommerce.service.agent.refund.data.RefundRiskDataService;
import com.shixi.ecommerce.service.agent.refund.data.RefundRiskProfile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ScoreRefundRiskSkill extends AbstractRefundSkill<RefundSkillOutput> {
    private static final BigDecimal MEDIUM_AMOUNT_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("3000");

    private final RefundOrderDataClient orderDataClient;
    private final RefundRiskDataService refundRiskDataService;

    public ScoreRefundRiskSkill(RefundOrderDataClient orderDataClient, RefundRiskDataService refundRiskDataService) {
        this.orderDataClient = orderDataClient;
        this.refundRiskDataService = refundRiskDataService;
    }

    @Override
    public String getName() {
        return RefundSkillNames.SCORE_RISK;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        String text = context.getMessage() == null ? "" : context.getMessage().toLowerCase(Locale.ROOT);
        Map<String, String> updates = new HashMap<>();
        List<String> reasons = new ArrayList<>();

        int riskScore = assessTextRisk(text, reasons);
        String orderNo = context.getSlot(RefundSlots.ORDER_NO);
        if (orderNo != null) {
            OrderRefundSnapshotResponse snapshot =
                    orderDataClient.getRefundSnapshot(orderNo).orElse(null);
            if (snapshot != null) {
                RefundRiskProfile profile = refundRiskDataService.load(snapshot.getUserId(), orderNo);
                riskScore = Math.max(riskScore, assessOrderRisk(snapshot, profile, reasons, updates));
            }
        }

        String riskLevel = toRiskLevel(riskScore);
        updates.put(RefundSlots.RISK_LEVEL, riskLevel);
        RefundDecision decision = riskScore > 0 ? RefundDecision.MANUAL_REVIEW : null;
        String prompt = "Risk level=" + riskLevel + ", reasons="
                + (reasons.isEmpty() ? "stable refund history" : String.join(", ", reasons));
        return new RefundSkillOutput(prompt, updates, decision);
    }

    private int assessOrderRisk(
            OrderRefundSnapshotResponse snapshot,
            RefundRiskProfile profile,
            List<String> reasons,
            Map<String, String> updates) {
        int score = 0;
        updates.put(RefundSlots.USER_ID, String.valueOf(snapshot.getUserId()));
        updates.put(RefundSlots.ORDER_STATUS, snapshot.getStatus().name());
        updates.put(RefundSlots.ORDER_AMOUNT, snapshot.getTotalAmount().toPlainString());
        updates.put(RefundSlots.TOTAL_AFTER_SALE_COUNT, String.valueOf(profile.totalAfterSaleCount()));
        updates.put(RefundSlots.RECENT_AFTER_SALE_COUNT, String.valueOf(profile.recentAfterSaleCount()));
        updates.put(RefundSlots.OPEN_AFTER_SALE_COUNT, String.valueOf(profile.openAfterSaleCount()));
        updates.put(RefundSlots.EXISTING_AFTER_SALE, String.valueOf(profile.existingAfterSaleTicket()));

        if (snapshot.getStatus() == OrderStatus.REFUNDING || snapshot.getStatus() == OrderStatus.REFUNDED) {
            score = Math.max(score, 2);
            reasons.add("order already has refund status");
        }
        if (snapshot.getTotalAmount().compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            score = Math.max(score, 2);
            reasons.add("high-value order");
        } else if (snapshot.getTotalAmount().compareTo(MEDIUM_AMOUNT_THRESHOLD) >= 0) {
            score = Math.max(score, 1);
            reasons.add("higher order amount");
        }
        if (profile.existingAfterSaleTicket()) {
            score = Math.max(score, 2);
            reasons.add("existing after-sale ticket for this order");
        }
        if (profile.openAfterSaleCount() >= 3) {
            score = Math.max(score, 2);
            reasons.add("multiple open after-sale tickets");
        } else if (profile.recentAfterSaleCount() >= 3 || profile.totalAfterSaleCount() >= 5) {
            score = Math.max(score, 1);
            reasons.add("frequent after-sale history");
        }
        return score;
    }

    private int assessTextRisk(String text, List<String> reasons) {
        if (containsAny(text, "\u6b3a\u8bc8", "\u7ea0\u7eb7", "chargeback", "fraud")) {
            reasons.add("fraud or chargeback keywords");
            return 2;
        }
        if (containsAny(text, "\u591a\u6b21", "\u9891\u7e41", "frequent", "many times")) {
            reasons.add("repeated refund keywords");
            return 1;
        }
        return 0;
    }

    private String toRiskLevel(int riskScore) {
        if (riskScore >= 2) {
            return "HIGH";
        }
        if (riskScore == 1) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
