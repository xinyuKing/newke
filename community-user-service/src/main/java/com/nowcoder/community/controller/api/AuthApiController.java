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

/**
 * Internal authentication API exposed to other services.
 *
 * <p>The mall auth service uses these endpoints to validate forum credentials and to create new
 * forum accounts. The controller intentionally returns normalized API payloads instead of web
 * page responses so other services can consume it reliably.</p>
 */
@RestController
public class AuthApiController {
    @Autowired
    private UserService userService;

    /**
     * Verifies username and password against the forum user system.
     *
     * <p>This endpoint does not create a forum session. It only validates the credential, upgrades
     * legacy password hashes when needed, and returns the forum user profile required by the mall
     * account synchronization flow.</p>
     *
     * @param request login request from another internal service
     * @return standardized user payload for downstream synchronization
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

    /**
     * Creates a forum user for cross-service registration scenarios.
     *
     * <p>The forum remains the system of record for account creation. External services submit
     * their registration request here and receive the final forum identity data that should be
     * mirrored locally.</p>
     *
     * @param request registration request from another internal service
     * @return newly created forum user payload
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
