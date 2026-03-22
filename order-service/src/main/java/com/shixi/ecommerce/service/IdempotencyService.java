package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.IdempotentRecord;
import com.shixi.ecommerce.repository.IdempotentRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {
    private static final String PREFIX = "idem:";

    private final StringRedisTemplate redisTemplate;
    private final IdempotentRecordRepository repository;

    public IdempotencyService(StringRedisTemplate redisTemplate, IdempotentRecordRepository repository) {
        this.redisTemplate = redisTemplate;
        this.repository = repository;
    }

    public boolean acquire(String bizKey, Duration ttl) {
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
            return false;
        }
        IdempotentRecord record = new IdempotentRecord();
        record.setBizKey(bizKey);
        try {
            repository.save(record);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        } catch (RuntimeException ex) {
            if (redisOk) {
                redisTemplate.delete(redisKey);
            }
            throw ex;
        }
    }

    public void release(String bizKey) {
        repository.findByBizKey(bizKey).ifPresent(repository::delete);
        redisTemplate.delete(buildKey(bizKey));
    }

    private String buildKey(String bizKey) {
        return PREFIX + bizKey;
    }
}
