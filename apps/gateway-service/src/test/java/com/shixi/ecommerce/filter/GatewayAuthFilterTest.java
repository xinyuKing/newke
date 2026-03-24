package com.shixi.ecommerce.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.config.GatewayAuthProperties;
import com.shixi.ecommerce.domain.Role;
import com.shixi.ecommerce.security.JwtTokenProvider;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class GatewayAuthFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    private GatewayAuthFilter gatewayAuthFilter;
    private GatewayAuthProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GatewayAuthProperties();
        properties.setPublicPaths(List.of("/api/products/**"));
        gatewayAuthFilter = new GatewayAuthFilter(tokenProvider, properties, new ObjectMapper());
    }

    @Test
    void publicRoutesStripSpoofedIdentityHeaders() {
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        GatewayFilterChain chain = exchange -> {
            forwardedRequest.set(exchange.getRequest());
            return Mono.empty();
        };
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products/1001")
                .header(properties.getUserIdHeader(), "999")
                .header(properties.getRoleHeader(), "ADMIN")
                .build();

        gatewayAuthFilter.filter(MockServerWebExchange.from(request), chain).block();

        assertNull(forwardedRequest.get().getHeaders().getFirst(properties.getUserIdHeader()));
        assertNull(forwardedRequest.get().getHeaders().getFirst(properties.getRoleHeader()));
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void protectedRoutesInjectValidatedIdentityHeaders() {
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        GatewayFilterChain chain = exchange -> {
            forwardedRequest.set(exchange.getRequest());
            return Mono.empty();
        };
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUserId("valid-token")).thenReturn(42L);
        when(tokenProvider.getRole("valid-token")).thenReturn(Role.USER);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/user/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .header(properties.getUserIdHeader(), "999")
                .header(properties.getRoleHeader(), "ADMIN")
                .build();

        gatewayAuthFilter.filter(MockServerWebExchange.from(request), chain).block();

        assertEquals("42", forwardedRequest.get().getHeaders().getFirst(properties.getUserIdHeader()));
        assertEquals("USER", forwardedRequest.get().getHeaders().getFirst(properties.getRoleHeader()));
    }
}
