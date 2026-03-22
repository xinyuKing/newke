package com.shixi.ecommerce.service.agent.refund;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class RefundContextService {
    private static final String CONTEXT_PREFIX = "refund:ctx:";
    private static final String DEDUP_PREFIX = "refund:dedup:";
    private static final Duration TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;

    public RefundContextService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RefundContext load(String sessionId) {
        RefundContext context = new RefundContext();
        context.setSessionId(sessionId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(CONTEXT_PREFIX + sessionId);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                context.putSlot(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        String decision = context.getSlot(RefundSlots.DECISION);
        if (decision != null) {
            try {
                context.setDecision(RefundDecision.valueOf(decision));
            } catch (IllegalArgumentException ignored) {
                context.setDecision(null);
            }
        }
        return context;
    }

    public void save(RefundContext context) {
        if (context == null || context.getSessionId() == null) {
            return;
        }
        String key = CONTEXT_PREFIX + context.getSessionId();
        Map<String, String> payload = new HashMap<>(context.getSlots());
        if (context.getDecision() != null) {
            payload.put(RefundSlots.DECISION, context.getDecision().name());
        }
        if (!payload.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, payload);
            redisTemplate.expire(key, TTL);
        }
    }

    public void clear(String sessionId) {
        redisTemplate.delete(CONTEXT_PREFIX + sessionId);
    }

    public boolean recordDedup(String sessionId, String messageHash, Duration ttl) {
        if (sessionId == null || messageHash == null) {
            return false;
        }
        String key = DEDUP_PREFIX + sessionId + ":" + messageHash;
        Boolean added = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(added);
    }
}
