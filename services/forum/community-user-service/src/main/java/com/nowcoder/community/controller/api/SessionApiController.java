package com.nowcoder.community.controller.api;

import com.nowcoder.community.dto.AuthUserResponse;
import com.nowcoder.community.dto.SessionLoginRequest;
import com.nowcoder.community.dto.SessionRegisterRequest;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.security.ForumCookieService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 闈㈠悜鍓嶅悗绔垎绂诲満鏅殑璁哄潧浼氳瘽鎺ュ彛銆?
 *
 * <p>璇ユ帶鍒跺櫒鎶婅鍧涘師鏈夌殑鐧诲綍銆佹敞鍐屻€佸綋鍓嶇敤鎴锋煡璇€佹敞閿€鑳藉姏灏佽涓?JSON API锛?
 * 渚涙暣鍚堝悗鐨勭粺涓€鍓嶇鐩存帴璋冪敤銆?/p>
 */
@RestController
public class SessionApiController implements CommunityConstant {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HostHolder hostHolder;
    private final ForumCookieService forumCookieService;

    public SessionApiController(
            UserService userService,
            RedisTemplate<String, Object> redisTemplate,
            HostHolder hostHolder,
            ForumCookieService forumCookieService) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.hostHolder = hostHolder;
        this.forumCookieService = forumCookieService;
    }

    /**
     * 澶勭悊 SPA 鐧诲綍銆?
     *
     * @param request 鐧诲綍鍙傛暟
     * @param httpRequest HTTP 璇锋眰锛岀敤浜庡洖鍐?CSRF Cookie
     * @param response HTTP 鍝嶅簲锛岀敤浜庡洖鍐?ticket Cookie
     * @param kaptchaOwner 楠岃瘉鐮佸綊灞?Cookie
     * @return 鐧诲綍缁撴灉
     */
    @PostMapping("/api/session/login")
    public ApiResponse<Object> login(
            @RequestBody SessionLoginRequest request,
            HttpServletRequest httpRequest,
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
            errorData.put("codeMsg", "楠岃瘉鐮侀敊璇紒");
            return ApiResponse.error(1, "login_failed", errorData);
        }

        int expiredSeconds = request.isRememberme() ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> loginResult =
                userService.login(request.getUsername(), request.getPassword(), expiredSeconds);
        if (!loginResult.containsKey("ticket")) {
            return ApiResponse.error(1, "login_failed", loginResult);
        }

        forumCookieService.writeTicketCookie(response, String.valueOf(loginResult.get("ticket")), expiredSeconds);
        forumCookieService.ensureCsrfCookie(httpRequest, response);

        User user = userService.findUserByName(request.getUsername());
        return ApiResponse.success(toAuthUser(user));
    }

    /**
     * 澶勭悊 SPA 娉ㄥ唽銆?
     *
     * @param request 娉ㄥ唽鍙傛暟
     * @return 娉ㄥ唽缁撴灉
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
     * 鏌ヨ褰撳墠璁哄潧浼氳瘽鐢ㄦ埛銆?
     *
     * @return 褰撳墠鐧诲綍鐢ㄦ埛鑴辨晱淇℃伅
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
     * 娉ㄩ攢褰撳墠璁哄潧浼氳瘽銆?
     *
     * @param ticket 鐧诲綍鍑瘉 Cookie
     * @param response HTTP 鍝嶅簲锛岀敤浜庢竻鐞嗘祻瑙堝櫒 Cookie
     * @return 娉ㄩ攢缁撴灉
     */
    @PostMapping("/api/session/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = "ticket", required = false) String ticket, HttpServletResponse response) {
        if (StringUtils.isNotBlank(ticket)) {
            userService.logout(ticket);
        }
        SecurityContextHolder.clearContext();
        forumCookieService.clearTicketCookie(response);
        forumCookieService.clearCsrfCookie(response);
        forumCookieService.clearCaptchaCookie(response);
        return ApiResponse.success(null);
    }

    /**
     * 鎶婅鍧涚敤鎴峰疄浣撹浆鎹负鍓嶇鍙嬪ソ鐨勮劚鏁忕粨鏋溿€?
     *
     * @param user 璁哄潧鐢ㄦ埛瀹炰綋
     * @return 缁熶竴鐢ㄦ埛鍝嶅簲
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
                user.getHeaderUrl());
    }
}
