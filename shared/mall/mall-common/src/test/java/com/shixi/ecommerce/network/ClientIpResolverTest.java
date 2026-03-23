package com.shixi.ecommerce.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {
    @Test
    void shouldTrustForwardedHeadersOnlyFromTrustedProxy() {
        ClientIpProperties properties = new ClientIpProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.10, 127.0.0.1");

        assertEquals("198.51.100.10", resolver.resolve(request));
    }

    @Test
    void shouldIgnoreForwardedHeadersFromUntrustedSource() {
        ClientIpProperties properties = new ClientIpProperties();
        ClientIpResolver resolver = new ClientIpResolver(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.5");
        request.addHeader("X-Forwarded-For", "198.51.100.10");

        assertEquals("203.0.113.5", resolver.resolve(request));
    }
}
