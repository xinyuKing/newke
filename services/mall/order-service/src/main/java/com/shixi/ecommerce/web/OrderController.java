package com.shixi.ecommerce.web;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.CreateOrderRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.dto.ShipOrderRequest;
import com.shixi.ecommerce.dto.TrackingResponse;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.LogisticsService;
import com.shixi.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final CurrentUserService currentUserService;
    private final LogisticsService logisticsService;

    public OrderController(OrderService orderService,
                           CurrentUserService currentUserService,
                           LogisticsService logisticsService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
        this.logisticsService = logisticsService;
    }

    @PostMapping
    public ApiResponse<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        request.setUserId(currentUserService.getCurrentUser().getUserId());
        return ApiResponse.ok(orderService.createOrder(request));
    }

    @PostMapping("/{orderNo}/pay")
    public ApiResponse<String> payOrder(@PathVariable String orderNo) {
        String role = currentUserService.getCurrentUser().getRole();
        orderService.payOrder(orderNo, role);
        return ApiResponse.ok("PAID");
    }

    @PostMapping("/{orderNo}/ship")
    public ApiResponse<String> shipOrder(@PathVariable String orderNo,
                                         @Valid @RequestBody ShipOrderRequest request) {
        String role = currentUserService.getCurrentUser().getRole();
        orderService.shipOrder(orderNo, request.getCarrierCode(), request.getTrackingNo(), role);
        return ApiResponse.ok("SHIPPED");
    }

    @PostMapping("/{orderNo}/confirm")
    public ApiResponse<String> confirmReceipt(@PathVariable String orderNo) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        String role = currentUserService.getCurrentUser().getRole();
        orderService.confirmReceipt(userId, orderNo, role);
        return ApiResponse.ok("COMPLETED");
    }

    @GetMapping("/{orderNo}/tracking")
    public ApiResponse<TrackingResponse> tracking(@PathVariable String orderNo) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(logisticsService.query(userId, orderNo));
    }
}
