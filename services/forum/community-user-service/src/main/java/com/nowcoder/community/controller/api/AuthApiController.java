package com.nowcoder.community.controller.api;

import com.nowcoder.community.dto.AuthLoginRequest;
import com.nowcoder.community.dto.AuthRegisterRequest;
import com.nowcoder.community.dto.AuthUserResponse;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面向其他内部服务的认证接口。
 *
 * <p>商城侧认证服务通过该控制器复用论坛用户体系，实现“论坛为主、商城镜像”的账号融合模式。</p>
 */
@RestController
public class AuthApiController {

    private final UserService userService;

    public AuthApiController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 校验论坛账号密码。
     *
     * @param request 内部登录请求
     * @return 标准化用户信息
     */
    @PostMapping("/api/auth/login")
    public ApiResponse<AuthUserResponse> login(@RequestBody AuthLoginRequest request) {
        if (request == null || StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getPassword())) {
            return ApiResponse.error(400, "invalid_request");
        }

        User user = userService.findUserByName(request.getUsername());
        if (user == null) {
            return ApiResponse.error(404, "user_not_found");
        }
        if (user.getStatus() == 0) {
            return ApiResponse.error(403, "user_not_activated");
        }
        if (!userService.matchesPassword(user, request.getPassword())) {
            return ApiResponse.error(401, "password_incorrect");
        }

        userService.upgradePasswordIfNeeded(user, request.getPassword());
        return ApiResponse.success(toAuthUser(user));
    }

    /**
     * 创建论坛账号，供其他内部服务完成统一注册流程。
     *
     * @param request 内部注册请求
     * @return 新建论坛用户信息
     */
    @PostMapping("/api/auth/register")
    public ApiResponse<AuthUserResponse> register(@RequestBody AuthRegisterRequest request) {
        if (request == null || StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getPassword())) {
            return ApiResponse.error(400, "invalid_request");
        }

        User user;
        try {
            user = userService.registerExternal(request);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error(400, ex.getMessage());
        }
        return ApiResponse.success(toAuthUser(user));
    }

    /**
     * 转换统一的认证返回对象。
     *
     * @param user 论坛用户实体
     * @return 认证响应
     */
    private AuthUserResponse toAuthUser(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getType(),
                user.getStatus(),
                user.getEmail(),
                user.getHeaderUrl()
        );
    }
}
