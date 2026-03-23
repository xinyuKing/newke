package com.nowcoder.community.controller.api;

import com.nowcoder.community.dto.AuthUserResponse;
import com.nowcoder.community.dto.SessionLoginRequest;
import com.nowcoder.community.dto.SessionRegisterRequest;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 面向前后端分离场景的论坛会话接口。
 *
 * <p>该控制器把论坛原有的登录、注册、当前用户查询、注销能力封装为 JSON API，
 * 供整合后的统一前端直接调用。</p>
 */
@RestController
public class SessionApiController implements CommunityConstant {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HostHolder hostHolder;

    public SessionApiController(UserService userService,
                                RedisTemplate<String, Object> redisTemplate,
                                HostHolder hostHolder) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.hostHolder = hostHolder;
    }

    /**
     * 处理 SPA 登录。
     *
     * @param request 登录参数
     * @param response HTTP 响应，用于回写 ticket Cookie
     * @param kaptchaOwner 验证码归属 Cookie
     * @return 登录结果
     */
    @PostMapping("/api/session/login")
    public ApiResponse<Object> login(@RequestBody SessionLoginRequest request,
                                     HttpServletResponse response,
                                     @CookieValue(value = "kaptchaOwner", required = false) String kaptchaOwner) {
        if (request == null) {
            return ApiResponse.error(400, "invalid_request");
        }

        Map<String, Object> errorData = new HashMap<>();
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            Object cachedValue = redisTemplate.opsForValue().get(RedisKeyUtil.getKaptchaKey(kaptchaOwner));
            if (cachedValue instanceof String) {
                kaptcha = (String) cachedValue;
            }
        }
        if (StringUtils.isBlank(kaptcha)
                || StringUtils.isBlank(request.getCode())
                || !StringUtils.equalsIgnoreCase(kaptcha, request.getCode())) {
            errorData.put("codeMsg", "验证码错误！");
            return ApiResponse.error(1, "login_failed", errorData);
        }

        int expiredSeconds = request.isRememberme() ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> loginResult = userService.login(
                request.getUsername(),
                request.getPassword(),
                expiredSeconds
        );
        if (!loginResult.containsKey("ticket")) {
            return ApiResponse.error(1, "login_failed", loginResult);
        }

        Cookie cookie = new Cookie("ticket", String.valueOf(loginResult.get("ticket")));
        cookie.setPath("/");
        cookie.setMaxAge(expiredSeconds);
        response.addCookie(cookie);

        User user = userService.findUserByName(request.getUsername());
        return ApiResponse.success(toAuthUser(user));
    }

    /**
     * 处理 SPA 注册。
     *
     * @param request 注册参数
     * @return 注册结果
     */
    @PostMapping("/api/session/register")
    public ApiResponse<Object> register(@RequestBody SessionRegisterRequest request) {
        if (request == null) {
            return ApiResponse.error(400, "invalid_request");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());

        Map<String, Object> result = userService.register(user);
        if (result == null || result.isEmpty()) {
            return ApiResponse.success(null);
        }
        return ApiResponse.error(1, "register_failed", result);
    }

    /**
     * 查询当前论坛会话用户。
     *
     * @return 当前登录用户脱敏信息
     */
    @GetMapping("/api/session/me")
    public ApiResponse<AuthUserResponse> me() {
        User user = hostHolder.getUser();
        if (user == null) {
            return ApiResponse.error(403, "not_login");
        }
        return ApiResponse.success(toAuthUser(user));
    }

    /**
     * 注销当前论坛会话。
     *
     * @param ticket 登录凭证 Cookie
     * @param response HTTP 响应，用于清理浏览器 Cookie
     * @return 注销结果
     */
    @PostMapping("/api/session/logout")
    public ApiResponse<Void> logout(@CookieValue(value = "ticket", required = false) String ticket,
                                    HttpServletResponse response) {
        if (StringUtils.isNotBlank(ticket)) {
            userService.logout(ticket);
        }
        SecurityContextHolder.clearContext();

        Cookie cookie = new Cookie("ticket", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ApiResponse.success(null);
    }

    /**
     * 把论坛用户实体转换为前端友好的脱敏结果。
     *
     * @param user 论坛用户实体
     * @return 统一用户响应
     */
    private AuthUserResponse toAuthUser(User user) {
        if (user == null) {
            return null;
        }
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
