package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.IdempotentRecord;
import com.shixi.ecommerce.repository.IdempotentRecordRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String PREFIX = "idem:";

    private final StringRedisTemplate redisTemplate;
    private final IdempotentRecordRepository repository;

    public IdempotencyService(StringRedisTemplate redisTemplate, IdempotentRecordRepository repository) {
        this.redisTemplate = redisTemplate;
        this.repository = repository;
    }

    public AcquireResult acquire(String bizKey, Duration ttl) {
        String redisKey = buildKey(bizKey);
        boolean redisOk = false;
        boolean redisError = false;
        try {
            redisOk = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, "1", ttl));
        } catch (Exception ignored) {
            // Allow DB fallback when Redis is unavailable
            redisError = true;
        }
        if (!redisError && !redisOk) {
            return existingResult(bizKey);
        }
        IdempotentRecord record = new IdempotentRecord();
        record.setBizKey(bizKey);
        try {
            repository.save(record);
            return AcquireResult.acquired();
        } catch (DataIntegrityViolationException ex) {
            if (redisOk) {
                deleteRedisKeyQuietly(redisKey);
            }
            return existingResult(bizKey);
        } catch (RuntimeException ex) {
            if (redisOk) {
                deleteRedisKeyQuietly(redisKey);
            }
            throw ex;
        }
    }

    public void complete(String bizKey, String responsePayload) {
        IdempotentRecord record = repository.findByBizKey(bizKey).orElseThrow();
        record.setCompleted(true);
        record.setResponsePayload(responsePayload);
        record.setCompletedAt(LocalDateTime.now());
        repository.save(record);
        deleteRedisKeyQuietly(buildKey(bizKey));
    }

    public void release(String bizKey) {
        repository.findByBizKey(bizKey).ifPresent(repository::delete);
        deleteRedisKeyQuietly(buildKey(bizKey));
    }

    private AcquireResult existingResult(String bizKey) {
        return repository
                .findByBizKey(bizKey)
                .map(record -> {
                    if (record.isCompleted()
                            && record.getResponsePayload() != null
                            && !record.getResponsePayload().isBlank()) {
                        return AcquireResult.replay(record.getResponsePayload());
                    }
                    return AcquireResult.inProgress();
                })
                .orElseGet(AcquireResult::inProgress);
    }

    private String buildKey(String bizKey) {
        return PREFIX + bizKey;
    }

    private void deleteRedisKeyQuietly(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (RuntimeException ex) {
            log.warn("Failed to delete idempotency key {}", redisKey, ex);
        }
    }

    public record AcquireResult(boolean locked, String replayPayload) {
        public static AcquireResult acquired() {
            return new AcquireResult(true, null);
        }

        public static AcquireResult replay(String replayPayload) {
            return new AcquireResult(false, replayPayload);
        }

        public static AcquireResult inProgress() {
            return new AcquireResult(false, null);
        }

        public boolean shouldReplay() {
            return replayPayload != null && !replayPayload.isBlank();
        }
    }
}
