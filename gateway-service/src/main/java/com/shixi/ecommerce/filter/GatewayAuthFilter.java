package com.shixi.ecommerce.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.config.GatewayAuthProperties;
import com.shixi.ecommerce.security.JwtTokenProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关统一鉴权过滤器，校验 JWT 并透传用户信息。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {
    private final JwtTokenProvider tokenProvider;
    private final GatewayAuthProperties properties;
    private final ObjectMapper objectMapper;

    public GatewayAuthFilter(JwtTokenProvider tokenProvider,
                             GatewayAuthProperties properties,
                             ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethodValue())) {
            return chain.filter(exchange);
        }
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }
        String token = auth.substring(7);
        if (!tokenProvider.validateToken(token)) {
            return unauthorized(exchange);
        }
        Long userId = tokenProvider.getUserId(token);
        String role = tokenProvider.getRole(token).name();

        return chain.filter(exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(properties.getUserIdHeader(), userId == null ? "" : String.valueOf(userId))
                        .header(properties.getRoleHeader(), role)
                        .build())
                .build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicPath(String path) {
        for (String pattern : properties.getPublicPaths()) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Unauthorized");
        try {
            byte[] bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes)));
        } catch (Exception ex) {
            return exchange.getResponse().setComplete();
        }
    }
}
