package com.shixi.ecommerce.web;

import com.shixi.ecommerce.dto.CreateOrderItemsRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单内部接口，供购物车服务调用。
 *
 * @author shixi
 * @date 2026-03-20
 */
@RestController
@RequestMapping("/internal/orders")
public class OrderInternalController {
    private final OrderService orderService;

    public OrderInternalController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单（来自购物车结算）。
     *
     * @param request 下单请求
     * @return 下单结果
     */
    @PostMapping
    public CreateOrderResponse create(@Valid @RequestBody CreateOrderItemsRequest request) {
        return orderService.createOrderByItems(
                request.getUserId(),
                request.getIdempotencyKey(),
                "order:cart",
                request.getItems()
        );
    }
}
