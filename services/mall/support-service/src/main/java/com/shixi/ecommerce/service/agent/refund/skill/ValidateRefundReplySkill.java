package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.service.agent.refund.FeedbackResult;
import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundContextValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ValidateRefundReplySkill extends AbstractRefundSkill<FeedbackResult> {
    private final RefundContextValidator contextValidator;

    public ValidateRefundReplySkill(RefundContextValidator contextValidator) {
        this.contextValidator = contextValidator;
    }

    @Override
    public String getName() {
        return RefundSkillNames.VALIDATE_REPLY;
    }

    @Override
    public FeedbackResult execute(RefundSkillRequest request) {
        RefundContext context = requireContext(request);
        SessionState state = requirePlan(request).getNextState();
        String reply = request.getReply();
        if (reply == null || reply.isBlank()) {
            return new FeedbackResult(false, "Empty reply");
        }

        String text = reply.toLowerCase(Locale.ROOT);
        if (contextValidator != null) {
            List<String> errors = contextValidator.validate(context, state);
            errors.addAll(contextValidator.validateDecision(context, context.getDecision()));
            if (!errors.isEmpty()) {
                return new FeedbackResult(false, String.join("; ", errors));
            }
        }

        if (state == SessionState.WAIT_ORDER && !containsAny(text, "order", "\u8ba2\u5355", "\u5355\u53f7")) {
            return new FeedbackResult(false, "Must ask for order number");
        }
        if (state == SessionState.WAIT_REASON && !containsAny(text, "reason", "\u539f\u56e0")) {
            return new FeedbackResult(false, "Must ask for refund reason");
        }
        if (state == SessionState.WAIT_EVIDENCE
                && !containsAny(text, "photo", "video", "evidence", "\u7167\u7247", "\u51ed\u8bc1", "\u56fe\u7247")) {
            return new FeedbackResult(false, "Must request evidence");
        }
        return new FeedbackResult(true, "OK");
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
