package com.shixi.ecommerce.service.agent.refund.skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RefundSkillRegistry {
    private final Map<String, RefundSkill<?>> skillsByName = new HashMap<>();

    public RefundSkillRegistry(List<RefundSkill<?>> skills) {
        for (RefundSkill<?> skill : skills) {
            RefundSkill<?> previous = skillsByName.put(skill.getName(), skill);
            if (previous != null) {
                throw new IllegalStateException("Duplicate refund skill: " + skill.getName());
            }
        }
    }

    public <T> T execute(String name, RefundSkillRequest request, Class<T> resultType) {
        RefundSkill<?> skill = skillsByName.get(name);
        if (skill == null) {
            throw new IllegalStateException("Refund skill not found: " + name);
        }
        Object result = skill.execute(request);
        if (result == null) {
            return null;
        }
        if (!resultType.isInstance(result)) {
            throw new IllegalStateException(
                    "Refund skill " + name + " returned " + result.getClass().getSimpleName());
        }
        return resultType.cast(result);
    }
}
