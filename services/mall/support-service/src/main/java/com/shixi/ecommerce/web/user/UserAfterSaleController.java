package com.shixi.ecommerce.web.user;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.domain.AfterSaleStatus;
import com.shixi.ecommerce.dto.AfterSaleCreateRequest;
import com.shixi.ecommerce.dto.AfterSaleResponse;
import com.shixi.ecommerce.security.JwtUser;
import com.shixi.ecommerce.service.AfterSaleService;
import com.shixi.ecommerce.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户售后接口。
 *
 * @author shixi
 * @date 2026-03-20
 */
@RestController
@RequestMapping("/api/user/after-sale")
public class UserAfterSaleController {
    private final AfterSaleService afterSaleService;
    private final CurrentUserService currentUserService;

    public UserAfterSaleController(AfterSaleService afterSaleService, CurrentUserService currentUserService) {
        this.afterSaleService = afterSaleService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ApiResponse<AfterSaleResponse> create(@Valid @RequestBody AfterSaleCreateRequest request) {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(afterSaleService.create(user.getUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<AfterSaleResponse>> list(@RequestParam(required = false) AfterSaleStatus status) {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(afterSaleService.listUser(user.getUserId(), status));
    }
}