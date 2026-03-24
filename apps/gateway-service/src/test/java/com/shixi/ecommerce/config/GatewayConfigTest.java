package com.shixi.ecommerce.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.security.JwtTokenProvider;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

@ExtendWith(MockitoExtension.class)
class GatewayConfigTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    private KeyResolver keyResolver;

    @BeforeEach
    void setUp() {
        keyResolver = new GatewayConfig().userOrIpKeyResolver(tokenProvider);
    }

    @Test
    void keyResolverUsesValidatedJwtInsteadOfSpoofedIdentityHeaders() {
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUserId("valid-token")).thenReturn(42L);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/user/orders")
                .remoteAddress(new InetSocketAddress("203.0.113.5", 8080))
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .header("X-User-Id", "999")
                .header("X-Forwarded-For", "198.51.100.10")
                .build();

        String key = keyResolver.resolve(MockServerWebExchange.from(request)).block();

        assertEquals("user:42", key);
    }

    @Test
    void keyResolverFallsBackToRemoteAddressForAnonymousTraffic() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/products/1")
                .remoteAddress(new InetSocketAddress("203.0.113.5", 8080))
                .header("X-User-Id", "999")
                .header("X-Forwarded-For", "198.51.100.10")
                .build();

        String key = keyResolver.resolve(MockServerWebExchange.from(request)).block();

        assertEquals("ip:203.0.113.5", key);
    }
}
