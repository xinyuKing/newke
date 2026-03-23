package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundDecision;
import com.shixi.ecommerce.service.agent.refund.RefundSlots;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ScoreRefundRiskSkill extends AbstractRefundSkill<RefundSkillOutput> {
    @Override
    public String getName() {
        return RefundSkillNames.SCORE_RISK;
    }

    @Override
    public RefundSkillOutput execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        String text = context.getMessage() == null ? "" : context.getMessage().toLowerCase(Locale.ROOT);
        String riskLevel = "LOW";
        if (containsAny(text, "\u591a\u6b21", "\u9891\u7e41", "frequent", "many times")) {
            riskLevel = "MEDIUM";
        }
        if (containsAny(text, "\u6b3a\u8bc8", "\u7ea0\u7eb7", "chargeback", "fraud")) {
            riskLevel = "HIGH";
        }

        Map<String, String> updates = new HashMap<>();
        updates.put(RefundSlots.RISK_LEVEL, riskLevel);
        RefundDecision decision = "HIGH".equals(riskLevel) || "MEDIUM".equals(riskLevel)
                ? RefundDecision.MANUAL_REVIEW
                : null;
        String prompt = "Risk level=" + riskLevel + ". Apply manual review if high value.";
        return new RefundSkillOutput(prompt, updates, decision);
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
