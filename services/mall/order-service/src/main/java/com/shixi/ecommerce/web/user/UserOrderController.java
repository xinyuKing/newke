package com.shixi.ecommerce.web.user;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.dto.CursorPageResponse;
import com.shixi.ecommerce.dto.OrderDetailResponse;
import com.shixi.ecommerce.dto.OrderLineItem;
import com.shixi.ecommerce.dto.OrderSummaryResponse;
import com.shixi.ecommerce.dto.TrackingResponse;
import com.shixi.ecommerce.dto.UserPurchaseRequest;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.LogisticsService;
import com.shixi.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/orders")
public class UserOrderController {
    private final OrderService orderService;
    private final CurrentUserService currentUserService;
    private final LogisticsService logisticsService;

    public UserOrderController(
            OrderService orderService, CurrentUserService currentUserService, LogisticsService logisticsService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
        this.logisticsService = logisticsService;
    }

    @PostMapping("/purchase")
    public ApiResponse<CreateOrderResponse> purchase(@Valid @RequestBody UserPurchaseRequest request) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        OrderLineItem item = new OrderLineItem(request.getSkuId(), request.getQuantity(), BigDecimal.ZERO);
        CreateOrderResponse response =
                orderService.createOrderByItems(userId, request.getIdempotencyKey(), "order:direct", List.of(item));
        return ApiResponse.ok(response);
    }

    @GetMapping
    public ApiResponse<CursorPageResponse<OrderSummaryResponse>> list(
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime cursorTime) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(orderService.listOrdersCursor(userId, cursorTime, cursorId, size));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetailResponse> detail(@PathVariable String orderNo) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(orderService.getOrderDetail(userId, orderNo));
    }

    @PostMapping("/{orderNo}/pay")
    public ApiResponse<String> payOrder(@PathVariable String orderNo) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        orderService.payOrder(userId, orderNo);
        return ApiResponse.ok("PAID");
    }

    @PostMapping("/{orderNo}/confirm")
    public ApiResponse<String> confirmReceipt(@PathVariable String orderNo) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        orderService.confirmReceipt(userId, orderNo);
        return ApiResponse.ok("COMPLETED");
    }

    @GetMapping("/{orderNo}/tracking")
    public ApiResponse<TrackingResponse> tracking(@PathVariable String orderNo) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(logisticsService.query(userId, orderNo));
    }
}
