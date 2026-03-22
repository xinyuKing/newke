package com.shixi.ecommerce.web.merchant;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.CursorPageResponse;
import com.shixi.ecommerce.dto.ProductCreateRequest;
import com.shixi.ecommerce.dto.ProductResponse;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/merchant/products")
public class MerchantProductController {
    private final ProductService productService;
    private final CurrentUserService currentUserService;

    public MerchantProductController(ProductService productService, CurrentUserService currentUserService) {
        this.productService = productService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        Long merchantId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(productService.createProduct(merchantId, request));
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> list(@RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        Long merchantId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(productService.listByMerchant(merchantId, page, size));
    }

    @GetMapping("/cursor")
    public ApiResponse<CursorPageResponse<ProductResponse>> listCursor(@RequestParam(required = false)
                                                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                                       LocalDateTime cursorTime,
                                                                       @RequestParam(required = false) Long cursorId,
                                                                       @RequestParam(required = false) Integer size) {
        Long merchantId = currentUserService.getCurrentUser().getUserId();
        return ApiResponse.ok(productService.listByMerchantCursor(merchantId, cursorTime, cursorId, size));
    }
}
