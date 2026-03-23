package com.shixi.ecommerce.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.config.GatewayAuthProperties;
import com.shixi.ecommerce.security.JwtTokenProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一网关鉴权过滤器。
 *
 * <p>浏览器请求统一先进入网关，网关负责完成 JWT 校验并向下游服务透传用户身份信息，
 * 从而避免每个商城服务重复解析同一份令牌。</p>
 */
@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int FILTER_ORDER = -100;
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized";

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
        if (shouldSkipAuthentication(exchange)) {
            return chain.filter(exchange);
        }

        String token = extractBearerToken(exchange);
        if (token == null || !tokenProvider.validateToken(token)) {
            return unauthorized(exchange);
        }

        Long userId = tokenProvider.getUserId(token);
        String role = tokenProvider.getRole(token) == null ? "" : tokenProvider.getRole(token).name();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(properties.getUserIdHeader(), userId == null ? "" : String.valueOf(userId))
                .header(properties.getRoleHeader(), role)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    /**
     * 判断当前请求是否可以跳过鉴权。
     *
     * @param exchange 当前请求上下文
     * @return 是否跳过鉴权
     */
    private boolean shouldSkipAuthentication(ServerWebExchange exchange) {
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return true;
        }

        String path = exchange.getRequest().getPath().value();
        for (String pattern : properties.getPublicPaths()) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
                continue;
            }
            if (path.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求头中提取 Bearer Token。
     *
     * @param exchange 当前请求上下文
     * @return Token；如果不存在或格式非法则返回 {@code null}
     */
    private String extractBearerToken(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    /**
     * 统一输出 401 响应。
     *
     * @param exchange 当前请求上下文
     * @return 响应流
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> responseBody = new LinkedHashMap<>(2);
        responseBody.put("success", false);
        responseBody.put("message", UNAUTHORIZED_MESSAGE);

        try {
            byte[] bytes = objectMapper.writeValueAsString(responseBody).getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException ex) {
            return exchange.getResponse().setComplete();
        }
    }
}
