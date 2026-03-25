package com.shixi.ecommerce.service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DuplicateOrderGuardService {
    private static final String LOCK_PREFIX = "lock:order:duplicate:";
    private static final String RESPONSE_PREFIX = "cache:order:duplicate:";

    private final StringRedisTemplate redisTemplate;
    private final Duration guardTtl;

    public DuplicateOrderGuardService(
            StringRedisTemplate redisTemplate, @Value("${order.duplicate.guard-ttl:15s}") Duration guardTtl) {
        this.redisTemplate = redisTemplate;
        this.guardTtl = guardTtl;
    }

    public AcquireResult acquire(String fingerprintKey) {
        String responsePayload = getReplayPayload(fingerprintKey);
        if (responsePayload != null) {
            return AcquireResult.replay(responsePayload);
        }
        String token = UUID.randomUUID().toString();
        try {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey(fingerprintKey), token, guardTtl);
            if (!Boolean.TRUE.equals(locked)) {
                responsePayload = getReplayPayload(fingerprintKey);
                return responsePayload == null ? AcquireResult.inProgress() : AcquireResult.replay(responsePayload);
            }
            responsePayload = getReplayPayload(fingerprintKey);
            if (responsePayload != null) {
                release(fingerprintKey, token);
                return AcquireResult.replay(responsePayload);
            }
            return AcquireResult.acquired(token);
        } catch (RuntimeException ex) {
            return AcquireResult.acquired(null);
        }
    }

    public void complete(String fingerprintKey, String token, String responsePayload) {
        try {
            redisTemplate.opsForValue().set(responseKey(fingerprintKey), responsePayload, guardTtl);
        } finally {
            release(fingerprintKey, token);
        }
    }

    public void release(String fingerprintKey, String token) {
        if (token == null) {
            return;
        }
        try {
            String lockKey = lockKey(fingerprintKey);
            String currentToken = redisTemplate.opsForValue().get(lockKey);
            if (Objects.equals(token, currentToken)) {
                redisTemplate.delete(lockKey);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private String getReplayPayload(String fingerprintKey) {
        try {
            String payload = redisTemplate.opsForValue().get(responseKey(fingerprintKey));
            if (payload == null || payload.isBlank()) {
                return null;
            }
            return payload;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String lockKey(String fingerprintKey) {
        return LOCK_PREFIX + fingerprintKey;
    }

    private String responseKey(String fingerprintKey) {
        return RESPONSE_PREFIX + fingerprintKey;
    }

    public record AcquireResult(boolean locked, String lockToken, String replayPayload) {
        public static AcquireResult acquired(String lockToken) {
            return new AcquireResult(true, lockToken, null);
        }

        public static AcquireResult replay(String replayPayload) {
            return new AcquireResult(false, null, replayPayload);
        }

        public static AcquireResult inProgress() {
            return new AcquireResult(false, null, null);
        }

        public boolean shouldReplay() {
            return replayPayload != null && !replayPayload.isBlank();
        }
    }
}
