package com.nowcoder.community.dto;

/**
 * SPA 登录请求参数。
 *
 * <p>论坛原始的登录控制器主要服务于 Thymeleaf 页面提交流程，而前后端融合后，Vue
 * 前端需要通过 JSON 方式提交账号、密码与验证码，因此单独定义一个会话登录 DTO。</p>
 *
 * @author shixi
 * @date 2026-03-22
 */
public class SessionLoginRequest {
    /**
     * 论坛用户名。
     */
    private String username;

    /**
     * 明文密码，服务端会在登录流程内完成校验。
     */
    private String password;

    /**
     * 图形验证码内容。
     */
    private String code;

    /**
     * 是否启用“记住我”模式。
     */
    private boolean rememberme;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isRememberme() {
        return rememberme;
    }

    public void setRememberme(boolean rememberme) {
        this.rememberme = rememberme;
    }
}
