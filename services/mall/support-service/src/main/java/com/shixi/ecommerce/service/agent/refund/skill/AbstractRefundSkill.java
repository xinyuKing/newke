package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundTaskPlan;

public abstract class AbstractRefundSkill<T> implements RefundSkill<T> {
    protected RefundContext requireContext(RefundSkillRequest request) {
        if (request == null || request.getContext() == null) {
            throw new IllegalArgumentException("Refund skill requires context.");
        }
        return request.getContext();
    }

    protected RefundTaskPlan requirePlan(RefundSkillRequest request) {
        if (request == null || request.getPlan() == null) {
            throw new IllegalArgumentException("Refund skill requires task plan.");
        }
        return request.getPlan();
    }
}
