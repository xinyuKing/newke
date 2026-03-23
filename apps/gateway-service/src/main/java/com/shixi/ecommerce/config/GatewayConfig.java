package com.shixi.ecommerce.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * 网关通用配置。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Configuration
public class GatewayConfig {
    @Bean
    public KeyResolver userOrIpKeyResolver(GatewayAuthProperties properties) {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(properties.getUserIdHeader());
            if (StringUtils.hasText(userId)) {
                return Mono.just("user:" + userId);
            }
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (!StringUtils.hasText(ip) && exchange.getRequest().getRemoteAddress() != null) {
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just("ip:" + (ip == null ? "unknown" : ip));
        };
    }
}
