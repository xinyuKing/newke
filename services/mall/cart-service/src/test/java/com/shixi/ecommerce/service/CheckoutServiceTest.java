package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.dto.OrderLineItem;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CheckoutRecordService checkoutRecordService;

    @Mock
    private OrderClient orderClient;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        checkoutService = new CheckoutService(checkoutRecordService, orderClient);
    }

    @Test
    void checkoutKeepsSnapshotWhenOrderIsStillInProgress() {
        CheckoutRecordService.CheckoutSnapshot snapshot = new CheckoutRecordService.CheckoutSnapshot(
                List.of(new OrderLineItem(1001L, 1, new BigDecimal("19.90"))), false);
        when(checkoutRecordService.prepare(42L, "idem-1")).thenReturn(snapshot);
        when(orderClient.createOrder(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException("Duplicate order request in progress"));

        assertThrows(BusinessException.class, () -> checkoutService.checkout(42L, "idem-1"));

        verify(checkoutRecordService, never()).release(42L, "idem-1");
        verify(checkoutRecordService, never()).markCartCleared(42L, "idem-1");
    }

    @Test
    void checkoutReleasesSnapshotOnDeterministicBusinessFailure() {
        CheckoutRecordService.CheckoutSnapshot snapshot = new CheckoutRecordService.CheckoutSnapshot(
                List.of(new OrderLineItem(1001L, 1, new BigDecimal("19.90"))), false);
        when(checkoutRecordService.prepare(42L, "idem-1")).thenReturn(snapshot);
        when(orderClient.createOrder(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BusinessException("Insufficient stock"));

        assertThrows(BusinessException.class, () -> checkoutService.checkout(42L, "idem-1"));

        verify(checkoutRecordService).release(42L, "idem-1");
        verify(checkoutRecordService, never()).markCartCleared(42L, "idem-1");
    }

    @Test
    void checkoutClearsCartOnceOrderSucceeds() {
        CheckoutRecordService.CheckoutSnapshot snapshot = new CheckoutRecordService.CheckoutSnapshot(
                List.of(new OrderLineItem(1001L, 1, new BigDecimal("19.90"))), false);
        when(checkoutRecordService.prepare(42L, "idem-1")).thenReturn(snapshot);
        when(orderClient.createOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CreateOrderResponse("ORD-1", null, List.of("ORD-1"), false));

        checkoutService.checkout(42L, "idem-1");

        verify(checkoutRecordService).markCartCleared(42L, "idem-1");
        verify(checkoutRecordService, never()).release(42L, "idem-1");
    }
}
