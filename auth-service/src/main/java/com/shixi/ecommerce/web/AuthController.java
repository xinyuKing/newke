package com.shixi.ecommerce.web;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.LoginRequest;
import com.shixi.ecommerce.dto.LoginResponse;
import com.shixi.ecommerce.dto.RegisterRequest;
import com.shixi.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok("OK");
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
