package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.OrderRateLimitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 下单限流服务，支持用户与 IP 维度。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class RateLimitService {
    private static final String USER_PREFIX = "rate:order:user:";
    private static final String IP_PREFIX = "rate:order:ip:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;
    private final OrderRateLimitProperties properties;

    public RateLimitService(StringRedisTemplate redisTemplate,
                            DefaultRedisScript<Long> rateLimitScript,
                            OrderRateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
        this.properties = properties;
    }

    /**
     * 用户维度限流。
     *
     * @param userId 用户 ID
     * @return 是否允许下单
     */
    public boolean allowUser(Long userId) {
        if (userId == null) {
            return true;
        }
        return allow(USER_PREFIX + userId, properties.getUserMax(), properties.getWindowSeconds());
    }

    /**
     * IP 维度限流。
     *
     * @param ip IP 地址
     * @return 是否允许下单
     */
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
