package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.security.ForumCookieService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
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
 * 璁哄潧鐧诲綍涓庢敞鍐岄〉闈㈡帶鍒跺櫒銆?
 */
@Controller
public class LoginController implements CommunityConstant {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    private final UserService userService;
    private final Producer kaptchaProducer;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ForumCookieService forumCookieService;

    public LoginController(
            UserService userService,
            Producer kaptchaProducer,
            RedisTemplate<String, Object> redisTemplate,
            ForumCookieService forumCookieService) {
        this.userService = userService;
        this.kaptchaProducer = kaptchaProducer;
        this.redisTemplate = redisTemplate;
        this.forumCookieService = forumCookieService;
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
     * 澶勭悊琛ㄥ崟鐧诲綍銆?
     *
     * @param model 椤甸潰妯″瀷
     * @param username 鐢ㄦ埛鍚?
     * @param password 瀵嗙爜
     * @param code 楠岃瘉鐮?
     * @param rememberme 鏄惁璁颁綇鐧诲綍鐘舵€?
     * @param request HTTP 璇锋眰
     * @param response HTTP 鍝嶅簲
     * @param kaptchaOwner 楠岃瘉鐮佸綊灞?Cookie
     * @return 椤甸潰璺宠浆缁撴灉
     */
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(
            Model model,
            String username,
            String password,
            String code,
            @RequestParam(name = "rememberme", defaultValue = "false") boolean rememberme,
            HttpServletRequest request,
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
            model.addAttribute("codeMsg", "楠岃瘉鐮侀敊璇紒");
            return "/site/login";
        }

        int expiredTime = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> result = userService.login(username, password, expiredTime);
        if (result.containsKey("ticket")) {
            forumCookieService.writeTicketCookie(response, String.valueOf(result.get("ticket")), expiredTime);
            forumCookieService.ensureCsrfCookie(request, response);
            return "redirect:/index";
        }

        model.addAttribute("usernameMsg", result.get("usernameMsg"));
        model.addAttribute("passwordMsg", result.get("passwordMsg"));
        return "/site/login";
    }

    /**
     * 澶勭悊璁哄潧鐢ㄦ埛娉ㄥ唽銆?
     *
     * @param model 椤甸潰妯″瀷
     * @param user 鐢ㄦ埛瀹炰綋
     * @return 椤甸潰璺宠浆缁撴灉
     */
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {
        Map<String, Object> result = userService.register(user);
        if (result == null || result.isEmpty()) {
            model.addAttribute("msg", "娉ㄥ唽鎴愬姛锛屾垜浠凡缁忓悜鎮ㄧ殑閭鍙戦€佷簡婵€娲婚偖浠讹紝璇峰敖蹇畬鎴愭縺娲汇€?");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }

        model.addAttribute("usernameMsg", result.get("usernameMsg"));
        model.addAttribute("passwordMsg", result.get("passwordMsg"));
        model.addAttribute("emailMsg", result.get("emailMsg"));
        return "/site/register";
    }

    /**
     * 澶勭悊婵€娲婚摼鎺ャ€?
     *
     * @param model 椤甸潰妯″瀷
     * @param userId 鐢ㄦ埛 ID
     * @param code 婵€娲荤爜
     * @return 椤甸潰璺宠浆缁撴灉
     */
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "鎮ㄧ殑璐﹀彿宸茬粡婵€娲绘垚鍔燂紝鍙互姝ｅ父鐧诲綍浣跨敤銆?");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "褰撳墠鐢ㄦ埛宸茬粡婵€娲伙紝璇风洿鎺ョ櫥褰曘€?");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "婵€娲诲け璐ワ紝鎮ㄦ彁渚涚殑婵€娲荤爜鏃犳晥銆?");
            model.addAttribute("target", "/register");
        }
        return "/site/operate-result";
    }

    /**
     * 鐢熸垚鍥惧舰楠岃瘉鐮併€?
     *
     * @param response HTTP 鍝嶅簲
     */
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response) {
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        String kaptchaOwner = CommunityUtil.generateUUID();
        forumCookieService.writeCaptchaCookie(response, kaptchaOwner, 60);
        redisTemplate.opsForValue().set(RedisKeyUtil.getKaptchaKey(kaptchaOwner), text, 60, TimeUnit.SECONDS);

        response.setContentType("image/png");
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException ex) {
            LOGGER.error("鐢熸垚楠岃瘉鐮佸け璐?", ex);
        }
    }

    /**
     * 娉ㄩ攢褰撳墠浼氳瘽銆?
     *
     * @param ticket 鐧诲綍鍑瘉
     * @param response HTTP 鍝嶅簲
     * @return 椤甸潰璺宠浆缁撴灉
     */
    @RequestMapping(path = "/logout", method = RequestMethod.POST)
    public String logout(@CookieValue(value = "ticket", required = false) String ticket, HttpServletResponse response) {
        if (StringUtils.isNotBlank(ticket)) {
            userService.logout(ticket);
        }
        SecurityContextHolder.clearContext();
        forumCookieService.clearTicketCookie(response);
        forumCookieService.clearCsrfCookie(response);
        forumCookieService.clearCaptchaCookie(response);
        return "redirect:/login";
    }
}
