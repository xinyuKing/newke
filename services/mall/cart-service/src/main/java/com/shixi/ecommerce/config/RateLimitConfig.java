package com.shixi.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * 限流 Lua 脚本配置。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Configuration
public class RateLimitConfig {
    @Bean
    public DefaultRedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/rate_limit.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
