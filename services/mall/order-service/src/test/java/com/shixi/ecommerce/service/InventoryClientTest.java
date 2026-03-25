package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.dto.OrderLineItem;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
    void deductBatchReturnsFalseForActualInsufficientStock() {
        when(restTemplate.postForObject(any(String.class), any(), eq(ApiResponse.class)))
                .thenReturn(ApiResponse.ok(false));

        boolean result = inventoryClient.deductBatch(List.of(new OrderLineItem(1001L, 2, BigDecimal.ZERO)));

        assertFalse(result);
    }

    @Test
    void deductBatchRejectsEmptyInventoryResponse() {
        when(restTemplate.postForObject(any(String.class), any(), eq(ApiResponse.class)))
                .thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> inventoryClient.deductBatch(List.of(new OrderLineItem(1001L, 2, BigDecimal.ZERO))));

        assertEquals("Inventory service unavailable", exception.getMessage());
    }

    @Test
    void deductBatchTranslatesClientErrorResponseBody() throws Exception {
        HttpClientErrorException httpException = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                objectMapper.writeValueAsBytes(ApiResponse.fail("SKU not found")),
                StandardCharsets.UTF_8);
        when(restTemplate.postForObject(any(String.class), any(), eq(ApiResponse.class)))
                .thenThrow(httpException);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> inventoryClient.deductBatch(List.of(new OrderLineItem(1001L, 2, BigDecimal.ZERO))));

        assertEquals("SKU not found", exception.getMessage());
    }

    @Test
    void releaseBatchRejectsEmptyInventoryResponse() {
        when(restTemplate.postForObject(any(String.class), any(), eq(ApiResponse.class)))
                .thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> inventoryClient.releaseBatch(List.of(new OrderLineItem(1001L, 2, BigDecimal.ZERO))));

        assertEquals("Inventory compensation failed", exception.getMessage());
    }
}
