package com.shixi.ecommerce.config;

import com.shixi.ecommerce.security.JwtTokenProvider;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
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
    public KeyResolver userOrIpKeyResolver(JwtTokenProvider tokenProvider) {
        return exchange -> {
            String userId = resolveAuthenticatedUserId(exchange, tokenProvider);
            if (StringUtils.hasText(userId)) {
                return Mono.just("user:" + userId);
            }
            return Mono.just("ip:" + resolveRemoteAddress(exchange));
        };
    }

    private String resolveAuthenticatedUserId(ServerWebExchange exchange, JwtTokenProvider tokenProvider) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length());
        if (!tokenProvider.validateToken(token)) {
            return null;
        }
        Long userId = tokenProvider.getUserId(token);
        return userId == null ? null : String.valueOf(userId);
    }

    private String resolveRemoteAddress(ServerWebExchange exchange) {
        if (exchange.getRequest().getRemoteAddress() == null
                || exchange.getRequest().getRemoteAddress().getAddress() == null) {
            return "unknown";
        }
        // Rate limiting must not trust client-supplied forwarding headers.
        return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }
}
