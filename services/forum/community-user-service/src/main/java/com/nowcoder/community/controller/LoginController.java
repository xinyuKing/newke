package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 论坛登录与注册页面控制器。
 */
@Controller
public class LoginController implements CommunityConstant {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    private final UserService userService;
    private final Producer kaptchaProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    public LoginController(
            UserService userService, Producer kaptchaProducer, RedisTemplate<String, Object> redisTemplate) {
        this.userService = userService;
        this.kaptchaProducer = kaptchaProducer;
        this.redisTemplate = redisTemplate;
    }

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    /**
     * 处理表单登录。
     *
     * @param model 页面模型
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param rememberme 是否记住登录状态
     * @param response HTTP 响应
     * @param kaptchaOwner 验证码归属 Cookie
     * @return 页面跳转结果
     */
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(
            Model model,
            String username,
            String password,
            String code,
            @RequestParam(name = "rememberme", defaultValue = "false") boolean rememberme,
            HttpServletResponse response,
            @CookieValue(value = "kaptchaOwner", required = false) String kaptchaOwner) {
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            Object cachedValue = redisTemplate.opsForValue().get(RedisKeyUtil.getKaptchaKey(kaptchaOwner));
            if (cachedValue instanceof String) {
                kaptcha = (String) cachedValue;
            }
        }

        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !StringUtils.equalsIgnoreCase(kaptcha, code)) {
            model.addAttribute("codeMsg", "验证码错误！");
            return "/site/login";
        }

        int expiredTime = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> result = userService.login(username, password, expiredTime);
        if (result.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", String.valueOf(result.get("ticket")));
            cookie.setPath("/");
            cookie.setMaxAge(expiredTime);
            response.addCookie(cookie);
            return "redirect:/index";
        }

        model.addAttribute("usernameMsg", result.get("usernameMsg"));
        model.addAttribute("passwordMsg", result.get("passwordMsg"));
        return "/site/login";
    }

    /**
     * 处理论坛用户注册。
     *
     * @param model 页面模型
     * @param user 用户实体
     * @return 页面跳转结果
     */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> result = userService.register(user);
        if (result == null || result.isEmpty()) {
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了激活邮件，请尽快完成激活。");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }

        model.addAttribute("usernameMsg", result.get("usernameMsg"));
        model.addAttribute("passwordMsg", result.get("passwordMsg"));
        model.addAttribute("emailMsg", result.get("emailMsg"));
        return "/site/register";
    }

    /**
     * 处理激活链接。
     *
     * @param model 页面模型
     * @param userId 用户 ID
     * @param code 激活码
     * @return 页面跳转结果
     */
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "您的账号已经激活成功，可以正常登录使用。");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "当前用户已经激活，请直接登录。");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败，您提供的激活码无效。");
            model.addAttribute("target", "/register");
        }
        return "/site/operate-result";
    }

    /**
     * 生成图形验证码。
     *
     * @param response HTTP 响应
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response) {
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath("/");
        response.addCookie(cookie);

        redisTemplate.opsForValue().set(RedisKeyUtil.getKaptchaKey(kaptchaOwner), text, 60, TimeUnit.SECONDS);

        response.setContentType("image/png");
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException ex) {
            LOGGER.error("生成验证码失败", ex);
        }
    }

    /**
     * 注销当前会话。
     *
     * @param ticket 登录凭证
     * @return 页面跳转结果
     */
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue(value = "ticket", required = false) String ticket) {
        if (StringUtils.isNotBlank(ticket)) {
            userService.logout(ticket);
        }
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }
}
