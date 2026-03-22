package com.shixi.ecommerce.web.user;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.dto.CursorPageResponse;
import com.shixi.ecommerce.dto.OrderDetailResponse;
import com.shixi.ecommerce.dto.OrderLineItem;
import com.shixi.ecommerce.dto.OrderSummaryResponse;
import com.shixi.ecommerce.dto.UserPurchaseRequest;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user/orders")
public class UserOrderController {
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public UserOrderController(OrderService orderService,
                               CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/purchase")
    public ApiResponse<CreateOrderResponse> purchase(@Valid @RequestBody UserPurchaseRequest request) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        OrderLineItem item = new OrderLineItem(request.getSkuId(), request.getQuantity(), BigDecimal.ZERO);
        CreateOrderResponse response = orderService.createOrderByItems(
                userId, request.getIdempotencyKey(), "order:direct", List.of(item));
        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<CursorPageResponse<OrderSummaryResponse>> list(@RequestParam(required = false) Integer size,
                                                                      @RequestParam(required = false) Long cursorId,
                                                                      @RequestParam(required = false)
                                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                      LocalDateTime cursorTime) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(orderService.listOrdersCursor(userId, cursorTime, cursorId, size));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetailResponse> detail(@PathVariable String orderNo) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(orderService.getOrderDetail(userId, orderNo));
    }
}
