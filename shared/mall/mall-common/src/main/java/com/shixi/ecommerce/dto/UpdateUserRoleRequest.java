package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.Role;
import jakarta.validation.constraints.NotNull;

public class UpdateUserRoleRequest {
    @NotNull
    private Role role;

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
