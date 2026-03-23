package com.shixi.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置属性。
 *
 * <p>统一维护公共白名单路径以及网关向下游透传的用户头信息。</p>
 */
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    /**
     * 无需鉴权的公共路径。
     */
    private List<String> publicPaths = new ArrayList<>();

    /**
     * 网关透传给下游的用户 ID 请求头。
     */
    private String userIdHeader = "X-User-Id";

    /**
     * 网关透传给下游的用户角色请求头。
     */
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
