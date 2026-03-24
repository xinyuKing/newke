package com.shixi.ecommerce.service.agent.refund.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.OrderStatus;
import com.shixi.ecommerce.internal.InternalAuthRestTemplateInterceptor;
import com.shixi.ecommerce.internal.InternalAuthSigner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RefundOrderDataClientTest {

    @Test
    void updateRefundStatusReturnsRemoteBusinessMessage() {
        RefundOrderDataClient client = new RefundOrderDataClient(
                new RestTemplateBuilder(),
                new InternalAuthRestTemplateInterceptor(mock(InternalAuthSigner.class)),
                "http://order-service",
                3000L);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://order-service/internal/orders/ORDER-1/refund-status"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"message\":\"Order already refunded\"}"));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> client.updateRefundStatus("ORDER-1", OrderStatus.REFUNDED));

        assertEquals("Order already refunded", exception.getMessage());
        server.verify();
    }
}
