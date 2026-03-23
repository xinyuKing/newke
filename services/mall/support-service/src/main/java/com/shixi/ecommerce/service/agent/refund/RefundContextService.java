package com.shixi.ecommerce.service.agent.refund;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.domain.SessionState;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefundContextService {
    private static final String CONTEXT_PREFIX = "refund:ctx:";
    private static final String DEDUP_PREFIX = "refund:dedup:";
    private static final String META_STATE = "_meta:state";
    private static final String META_FEEDBACK_HINTS = "_meta:feedbackHints";
    private static final Duration TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RefundContextService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public RefundContext load(String sessionId) {
        RefundContext context = new RefundContext();
        context.setSessionId(sessionId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(CONTEXT_PREFIX + sessionId);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            if (entry.getKey() != null
                    && entry.getValue() != null
                    && !isMetaKey(entry.getKey().toString())) {
                context.putSlot(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        Object decisionValue = entries.get(RefundSlots.DECISION);
        String decision = decisionValue == null ? null : decisionValue.toString();
        if (decision != null) {
            try {
                context.setDecision(RefundDecision.valueOf(decision));
            } catch (IllegalArgumentException ignored) {
                context.setDecision(null);
            }
        }
        restoreState(context, entries.get(META_STATE));
        restoreFeedbackHints(context, entries.get(META_FEEDBACK_HINTS));
        return context;
    }

    public void save(RefundContext context) {
        if (context == null || context.getSessionId() == null) {
            return;
        }
        String key = CONTEXT_PREFIX + context.getSessionId();
        Map<String, String> payload = new HashMap<>(context.getSlots());
        payload.remove(RefundSlots.DECISION);
        if (context.getDecision() != null) {
            payload.put(RefundSlots.DECISION, context.getDecision().name());
        }
        if (context.getState() != null) {
            payload.put(META_STATE, context.getState().name());
        }
        if (!context.getFeedbackHints().isEmpty()) {
            try {
                payload.put(META_FEEDBACK_HINTS, objectMapper.writeValueAsString(context.getFeedbackHints()));
            } catch (Exception ignored) {
                payload.put(META_FEEDBACK_HINTS, "[]");
            }
        }
        redisTemplate.delete(key);
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

    private void restoreState(RefundContext context, Object value) {
        if (value == null) {
            return;
        }
        try {
            context.setState(SessionState.valueOf(value.toString()));
        } catch (IllegalArgumentException ignored) {
            context.setState(null);
        }
    }

    private void restoreFeedbackHints(RefundContext context, Object value) {
        if (value == null) {
            return;
        }
        try {
            List<String> hints = objectMapper.readValue(value.toString(), new TypeReference<List<String>>() {});
            context.replaceFeedbackHints(hints);
        } catch (Exception ignored) {
            context.replaceFeedbackHints(List.of());
        }
    }

    private boolean isMetaKey(String key) {
        return META_STATE.equals(key) || META_FEEDBACK_HINTS.equals(key) || RefundSlots.DECISION.equals(key);
    }
}
