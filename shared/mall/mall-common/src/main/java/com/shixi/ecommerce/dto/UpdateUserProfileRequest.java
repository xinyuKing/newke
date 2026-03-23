package com.shixi.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户资料更新请求。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class UpdateUserProfileRequest {
    /** 昵称。 */
    @Size(max = 64)
    private String nickname;

    /** 头像 URL。 */
    @Size(max = 512)
    private String avatarUrl;

    /** 邮箱。 */
    @Email
    @Size(max = 128)
    private String email;

    /** 手机号。 */
    @Size(max = 32)
    private String phone;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
