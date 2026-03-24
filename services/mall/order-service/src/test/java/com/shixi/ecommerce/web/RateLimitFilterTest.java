package com.shixi.ecommerce.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.network.ClientIpResolver;
import com.shixi.ecommerce.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter =
                new RateLimitFilter(mock(RateLimitService.class), new ObjectMapper(), mock(ClientIpResolver.class));
    }

    @Test
    void shouldFilterDirectCreateOrderEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");

        assertFalse(rateLimitFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilterShipEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders/ORD-1/ship");

        assertTrue(rateLimitFilter.shouldNotFilter(request));
    }

    @Test
    void shouldContinueFilteringUserOrderPostEndpoints() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/user/orders/purchase");

        assertFalse(rateLimitFilter.shouldNotFilter(request));
    }
}
