package com.nowcoder.community.controller.api;

import com.nowcoder.community.dto.AuthLoginRequest;
import com.nowcoder.community.dto.AuthRegisterRequest;
import com.nowcoder.community.dto.AuthUserResponse;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthApiController {
    @Autowired
    private UserService userService;

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
        AuthUserResponse response = new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getType(),
                user.getStatus(),
                user.getEmail(),
                user.getHeaderUrl()
        );
        return ApiResponse.success(response);
    }

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
        AuthUserResponse response = new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getType(),
                user.getStatus(),
                user.getEmail(),
                user.getHeaderUrl()
        );
        return ApiResponse.success(response);
    }
}
