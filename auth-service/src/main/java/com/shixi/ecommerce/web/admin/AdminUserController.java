package com.shixi.ecommerce.web.admin;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.AdminUserResponse;
import com.shixi.ecommerce.dto.UpdateUserRoleRequest;
import com.shixi.ecommerce.dto.UpdateUserStatusRequest;
import com.shixi.ecommerce.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        return ApiResponse.ok(adminUserService.listUsers());
    }

    @PutMapping("/{userId}/role")
    public ApiResponse<String> updateRole(@PathVariable Long userId,
                                          @Valid @RequestBody UpdateUserRoleRequest request) {
        adminUserService.updateRole(userId, request.getRole());
        return ApiResponse.ok("OK");
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<String> updateStatus(@PathVariable Long userId,
                                            @Valid @RequestBody UpdateUserStatusRequest request) {
        adminUserService.updateStatus(userId, request.getEnabled());
        return ApiResponse.ok("OK");
    }
}
