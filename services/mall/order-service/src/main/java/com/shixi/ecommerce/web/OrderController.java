package com.shixi.ecommerce.web;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.CreateOrderRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.dto.ShipOrderRequest;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        request.setUserId(currentUserService.getCurrentUser().getUserId());
        return ApiResponse.ok(orderService.createOrder(request));
    }

    @PostMapping("/{orderNo}/ship")
    public ApiResponse<String> shipOrder(@PathVariable String orderNo, @Valid @RequestBody ShipOrderRequest request) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        String role = currentUserService.getCurrentUser().getRole();
        orderService.shipOrder(userId, orderNo, request.getCarrierCode(), request.getTrackingNo(), role);
        return ApiResponse.ok("SHIPPED");
    }
}
