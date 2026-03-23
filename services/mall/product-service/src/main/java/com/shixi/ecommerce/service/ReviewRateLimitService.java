package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.ReviewRateLimitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 评价接口限流服务，支持用户与 IP 维度。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ReviewRateLimitService {
    private static final String USER_PREFIX = "rate:review:user:";
    private static final String IP_PREFIX = "rate:review:ip:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;
    private final ReviewRateLimitProperties properties;

    public ReviewRateLimitService(StringRedisTemplate redisTemplate,
                                  DefaultRedisScript<Long> rateLimitScript,
                                  ReviewRateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
        this.properties = properties;
    }

    public boolean allowUser(Long userId) {
        if (userId == null) {
            return true;
        }
        return allow(USER_PREFIX + userId, properties.getUserMax(), properties.getWindowSeconds());
    }

    public boolean allowIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        return allow(IP_PREFIX + ip, properties.getIpMax(), properties.getWindowSeconds());
    }

    private boolean allow(String key, int limit, int windowSeconds) {
        Long result = redisTemplate.execute(rateLimitScript, Collections.singletonList(key),
                String.valueOf(limit), String.valueOf(windowSeconds), properties.getAlgorithm());
        return result != null && result == 1;
    }
}
