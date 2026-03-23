package com.shixi.ecommerce.service.agent.refund.skill;

public interface RefundSkill<T> {
    String getName();

    T execute(RefundSkillRequest request);
}
