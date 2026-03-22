package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.Role;

public class AdminUserResponse {
    private Long id;
    private String username;
    private Role role;
    private boolean enabled;

    public AdminUserResponse(Long id, String username, Role role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
