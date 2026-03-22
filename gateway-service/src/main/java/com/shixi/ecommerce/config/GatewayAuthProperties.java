package com.shixi.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {
    private List<String> publicPaths = new ArrayList<>();
    private String userIdHeader = "X-User-Id";
    private String roleHeader = "X-User-Role";

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }

    public String getUserIdHeader() {
        return userIdHeader;
    }

    public void setUserIdHeader(String userIdHeader) {
        this.userIdHeader = userIdHeader;
    }

    public String getRoleHeader() {
        return roleHeader;
    }

    public void setRoleHeader(String roleHeader) {
        this.roleHeader = roleHeader;
    }
}
