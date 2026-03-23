package com.shixi.ecommerce.dto;

import java.time.LocalDateTime;

/**
 * 用户资料响应。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class UserProfileResponse {
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String phone;
    private LocalDateTime createdAt;

    public UserProfileResponse(
            Long userId,
            String username,
            String nickname,
            String avatarUrl,
            String email,
            String phone,
            LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
