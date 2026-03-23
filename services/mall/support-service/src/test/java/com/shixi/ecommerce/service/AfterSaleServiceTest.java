package com.shixi.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.domain.AfterSaleTicket;
import com.shixi.ecommerce.dto.AfterSaleCreateRequest;
import com.shixi.ecommerce.dto.AfterSaleResponse;
import com.shixi.ecommerce.repository.AfterSaleTicketRepository;
import com.shixi.ecommerce.service.order.OrderAccessService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AfterSaleServiceTest {

    @Mock
    private AfterSaleTicketRepository repository;

    @Mock
    private OrderAccessService orderAccessService;

    private AfterSaleService afterSaleService;

    @BeforeEach
    void setUp() {
        afterSaleService = new AfterSaleService(repository, orderAccessService);
    }

    @Test
    void createRejectsDuplicateAfterSaleOrder() {
        AfterSaleCreateRequest request = request("ORD-1", "Damaged");
        when(repository.findByOrderNo("ORD-1")).thenReturn(Optional.of(new AfterSaleTicket()));

        assertThrows(BusinessException.class, () -> afterSaleService.create(42L, request));
        verify(orderAccessService).requireEligibleAfterSaleOrder(42L, "ORD-1");
    }

    @Test
    void createPersistsValidatedAfterSaleTicket() {
        AfterSaleCreateRequest request = request("ORD-1", "Damaged");
        when(repository.findByOrderNo("ORD-1")).thenReturn(Optional.empty());
        when(repository.save(any(AfterSaleTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AfterSaleResponse response = afterSaleService.create(42L, request);

        ArgumentCaptor<AfterSaleTicket> ticketCaptor = ArgumentCaptor.forClass(AfterSaleTicket.class);
        verify(repository).save(ticketCaptor.capture());
        AfterSaleTicket savedTicket = ticketCaptor.getValue();
        assertEquals(42L, savedTicket.getUserId());
        assertEquals("ORD-1", savedTicket.getOrderNo());
        assertEquals("Damaged", savedTicket.getReason());
        assertEquals(AfterSaleStatus.INIT, savedTicket.getStatus());
        assertEquals("ORD-1", response.getOrderNo());
        assertEquals(AfterSaleStatus.INIT, response.getStatus());
    }

    private AfterSaleCreateRequest request(String orderNo, String reason) {
        AfterSaleCreateRequest request = new AfterSaleCreateRequest();
        request.setOrderNo(orderNo);
        request.setReason(reason);
        return request;
    }
}
