package com.shixi.ecommerce.web.user;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.CartAddRequest;
import com.shixi.ecommerce.dto.CartItemResponse;
import com.shixi.ecommerce.dto.CartUpdateRequest;
import com.shixi.ecommerce.dto.CheckoutRequest;
import com.shixi.ecommerce.dto.CreateOrderResponse;
import com.shixi.ecommerce.service.CartService;
import com.shixi.ecommerce.service.CheckoutService;
import com.shixi.ecommerce.service.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/cart")
public class UserCartController {
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final CurrentUserService currentUserService;

    public UserCartController(
            CartService cartService, CheckoutService checkoutService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.checkoutService = checkoutService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/items")
    public ApiResponse<String> add(@Valid @RequestBody CartAddRequest request) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        cartService.addItem(userId, request.getSkuId(), request.getQuantity());
        return ApiResponse.ok("OK");
    }

    @PutMapping("/items/{skuId}")
    public ApiResponse<String> update(@PathVariable Long skuId, @Valid @RequestBody CartUpdateRequest request) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        cartService.updateItem(userId, skuId, request.getQuantity());
        return ApiResponse.ok("OK");
    }

    @GetMapping("/items")
    public ApiResponse<List<CartItemResponse>> list() {
        Long userId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(cartService.listItems(userId));
    }

    @DeleteMapping("/items/{skuId}")
    public ApiResponse<String> remove(@PathVariable Long skuId) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        cartService.removeItem(userId, skuId);
        return ApiResponse.ok("OK");
    }

    @PostMapping("/checkout")
    public ApiResponse<CreateOrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(checkoutService.checkout(userId, request.getIdempotencyKey()));
    }
}
