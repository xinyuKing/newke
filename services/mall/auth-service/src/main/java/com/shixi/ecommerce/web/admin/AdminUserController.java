package com.shixi.ecommerce.web.admin;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.AdminUserResponse;
import com.shixi.ecommerce.dto.UpdateUserRoleRequest;
import com.shixi.ecommerce.dto.UpdateUserStatusRequest;
import com.shixi.ecommerce.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城后台用户管理接口。
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 查询后台用户列表。
     *
     * @return 用户列表
     */
    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        return ApiResponse.ok(adminUserService.listUsers());
    }

    /**
     * 更新用户角色。
     *
     * @param userId  用户 ID
     * @param request 角色更新请求
     * @return 更新结果
     */
    @PutMapping("/{userId}/role")
    public ApiResponse<String> updateRole(
            @PathVariable Long userId, @Valid @RequestBody UpdateUserRoleRequest request) {
        adminUserService.updateRole(userId, request.getRole());
        return ApiResponse.ok("OK");
    }

    /**
     * 更新用户启停状态。
     *
     * @param userId  用户 ID
     * @param request 状态更新请求
     * @return 更新结果
     */
    @PutMapping("/{userId}/status")
    public ApiResponse<String> updateStatus(
            @PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request) {
        adminUserService.updateStatus(userId, request.getEnabled());
        return ApiResponse.ok("OK");
    }
}
