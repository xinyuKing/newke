package com.nowcoder.community.controller.api;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.ApiResponse;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController implements CommunityConstant {
    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private HostHolder hostHolder;


    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, Object> body,
                                                  @CookieValue(value = "kaptchaOwner", required = false) String kaptchaOwner,
                                                  HttpServletResponse response) {
        String username = body.get("username") == null ? null : body.get("username").toString();
        String password = body.get("password") == null ? null : body.get("password").toString();
        String code = body.get("code") == null ? null : body.get("code").toString();
        boolean rememberme = Boolean.parseBoolean(String.valueOf(body.getOrDefault("rememberme", "false")));
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(kaptchaKey);
        }

        if (StringUtils.isBlank(code) || StringUtils.isBlank(kaptcha) || !kaptcha.equalsIgnoreCase(code)) {
            return ApiResponse.error(1, "验证码错误");
        }

        int expiredTime = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredTime);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath("/");
            cookie.setMaxAge(expiredTime);
            response.addCookie(cookie);

            Map<String, Object> data = new HashMap<>();
            data.put("ticket", map.get("ticket"));
            data.put("user", userService.findUserByName(username));
            return ApiResponse.success(data);
        }

        return ApiResponse.error(1, "login_failed", map);
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody User user) {
        Map<String, Object> msg = userService.register(user);
        if (msg == null || msg.isEmpty()) {
            return ApiResponse.success(null);
        }
        return ApiResponse.error(1, "register_failed", msg);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@CookieValue(value = "ticket", required = false) String ticket,
                                    HttpServletResponse response) {
        if (StringUtils.isNotBlank(ticket)) {
            userService.logout(ticket);
            Cookie cookie = new Cookie("ticket", "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<User> me() {
        return ApiResponse.success(hostHolder.getUser());
    }
}
