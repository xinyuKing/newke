package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.CartItem;
import com.shixi.ecommerce.dto.CreateOrderItemsRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.dto.OrderLineItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车结算服务，负责将购物车条目转换为订单。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class CheckoutService {
    private final CartService cartService;
    private final OrderClient orderClient;

    public CheckoutService(CartService cartService, OrderClient orderClient) {
        this.cartService = cartService;
        this.orderClient = orderClient;
    }

    /**
     * 结算购物车：组装订单行并发起下单。
     *
     * @param userId          用户 ID
     * @param idempotencyKey  幂等键
     * @return 下单结果
     */
    @Transactional
    public CreateOrderResponse checkout(Long userId, String idempotencyKey) {
        List<CartItem> items = cartService.listEntityItems(userId);
        if (items.isEmpty()) {
            throw new BusinessException("Cart is empty");
        }
        List<OrderLineItem> orderItems = items.stream()
                .map(item -> new OrderLineItem(item.getSkuId(), item.getQuantity(), item.getPriceSnapshot()))
                .collect(Collectors.toList());
        CreateOrderItemsRequest request = new CreateOrderItemsRequest();
        request.setUserId(userId);
        request.setIdempotencyKey(idempotencyKey);
        request.setItems(orderItems);
        CreateOrderResponse response = orderClient.createOrder(request);
        cartService.clear(userId);
        return response;
    }
}
