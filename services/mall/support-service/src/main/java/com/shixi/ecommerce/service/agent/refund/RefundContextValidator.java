package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.SessionState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RefundContextValidator {
    public List<String> validate(RefundContext context, SessionState plannedState) {
        List<String> errors = new ArrayList<>();
        if (plannedState == SessionState.WAIT_ORDER && context.hasSlot(RefundSlots.ORDER_NO)) {
            errors.add("Order already provided but still asking order.");
        }
        if (plannedState == SessionState.WAIT_REASON && !context.hasSlot(RefundSlots.ORDER_NO)) {
            errors.add("Missing order number before asking reason.");
        }
        if (plannedState == SessionState.WAIT_EVIDENCE && !requiresEvidence(context)) {
            errors.add("Evidence requested but not required.");
        }
        if (plannedState == SessionState.REVIEWING && !context.hasSlot(RefundSlots.ORDER_NO)) {
            errors.add("Cannot review without order number.");
        }
        return errors;
    }

    public List<String> validateDecision(RefundContext context, RefundDecision decision) {
        List<String> errors = new ArrayList<>();
        if (decision == RefundDecision.APPROVE) {
            if (!context.hasSlot(RefundSlots.ORDER_NO)) {
                errors.add("Approve without order number.");
            }
            if (requiresEvidence(context) && !context.hasSlot(RefundSlots.EVIDENCE)) {
                errors.add("Approve without evidence.");
            }
        }
        if (decision == RefundDecision.REJECT && !context.hasSlot(RefundSlots.REASON)) {
            errors.add("Reject without reason.");
        }
        return errors;
    }

    private boolean requiresEvidence(RefundContext context) {
        String reason = context.getSlot(RefundSlots.REASON);
        return RefundReasonType.QUALITY.name().equals(reason)
                || RefundReasonType.WRONG_ITEM.name().equals(reason)
                || RefundReasonType.MISSING_PARTS.name().equals(reason);
    }
}
