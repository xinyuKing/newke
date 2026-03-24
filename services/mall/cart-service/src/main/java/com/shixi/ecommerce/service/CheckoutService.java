package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.dto.CreateOrderItemsRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import org.springframework.stereotype.Service;

/**
 * 购物车结算服务，负责将购物车条目转换为订单。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class CheckoutService {
    private static final String DUPLICATE_IN_PROGRESS_MESSAGE = "Duplicate order request in progress";

    private final CheckoutRecordService checkoutRecordService;
    private final OrderClient orderClient;

    public CheckoutService(CheckoutRecordService checkoutRecordService, OrderClient orderClient) {
        this.checkoutRecordService = checkoutRecordService;
        this.orderClient = orderClient;
    }

    /**
     * 结算购物车：组装订单行并发起下单。
     *
     * @param userId          用户 ID
     * @param idempotencyKey  幂等键
     * @return 下单结果
     */
    public CreateOrderResponse checkout(Long userId, String idempotencyKey) {
        CheckoutRecordService.CheckoutSnapshot snapshot = checkoutRecordService.prepare(userId, idempotencyKey);
        CreateOrderItemsRequest request = new CreateOrderItemsRequest();
        request.setUserId(userId);
        request.setIdempotencyKey(idempotencyKey);
        request.setItems(snapshot.items());
        try {
            CreateOrderResponse response = orderClient.createOrder(request);
            checkoutRecordService.markCartCleared(userId, idempotencyKey);
            return response;
        } catch (BusinessException ex) {
            if (!DUPLICATE_IN_PROGRESS_MESSAGE.equals(ex.getMessage())) {
                checkoutRecordService.release(userId, idempotencyKey);
            }
            throw ex;
        }
    }
}
