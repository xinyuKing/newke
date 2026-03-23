package com.shixi.ecommerce.web;

import com.shixi.ecommerce.dto.CreateOrderItemsRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.dto.OrderRefundSnapshotResponse;
import com.shixi.ecommerce.dto.TrackingResponse;
import com.shixi.ecommerce.service.LogisticsService;
import com.shixi.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final LogisticsService logisticsService;

    public OrderInternalController(OrderService orderService, LogisticsService logisticsService) {
        this.orderService = orderService;
        this.logisticsService = logisticsService;
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

    @GetMapping("/{orderNo}/refund-snapshot")
    public OrderRefundSnapshotResponse refundSnapshot(@PathVariable String orderNo) {
        return orderService.getRefundSnapshot(orderNo);
    }

    @GetMapping("/{orderNo}/tracking")
    public TrackingResponse tracking(@PathVariable String orderNo) {
        return logisticsService.queryInternal(orderNo);
    }
}
