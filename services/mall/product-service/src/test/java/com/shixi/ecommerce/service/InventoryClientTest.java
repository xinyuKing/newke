package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.common.BusinessException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class InventoryClientTest {

    @Mock
    private RestTemplate restTemplate;

    private InventoryClient inventoryClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        inventoryClient = new InventoryClient(restTemplate, "http://inventory-service", objectMapper);
    }

    @Test
    void initStockRejectsUnsuccessfulResponseBody() {
        when(restTemplate.postForObject(any(String.class), any(), eq(ApiResponse.class)))
                .thenReturn(ApiResponse.fail("Inventory init rejected"));

        BusinessException exception = assertThrows(BusinessException.class, () -> inventoryClient.initStock(1001L, 8));

        assertEquals("Inventory init rejected", exception.getMessage());
    }

    @Test
    void initStockTranslatesClientErrorResponseBody() throws Exception {
        HttpClientErrorException httpException = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                objectMapper.writeValueAsBytes(ApiResponse.fail("Invalid stock")),
                StandardCharsets.UTF_8);
        when(restTemplate.postForObject(any(String.class), any(), eq(ApiResponse.class)))
                .thenThrow(httpException);

        BusinessException exception = assertThrows(BusinessException.class, () -> inventoryClient.initStock(1001L, 8));

        assertEquals("Invalid stock", exception.getMessage());
    }

    @Test
    void initStockRejectsEmptyResponseBody() {
        when(restTemplate.postForObject(any(String.class), any(), eq(ApiResponse.class)))
                .thenReturn(null);

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> inventoryClient.initStock(1001L, 8));

        assertEquals("Inventory init failed", exception.getMessage());
    }
}
