package com.nowcoder.community.dto;

/**
 * SPA 注册请求参数。
 *
 * <p>该 DTO 只承载论坛前台注册所需的最小字段，避免把实体对象直接暴露给前端请求体。</p>
 *
 * @author shixi
 * @date 2026-03-22
 */
public class SessionRegisterRequest {
    /**
     * 用户名。
     */
    private String username;

    /**
     * 明文密码。
     */
    private String password;

    /**
     * 注册邮箱。
     */
    private String email;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
