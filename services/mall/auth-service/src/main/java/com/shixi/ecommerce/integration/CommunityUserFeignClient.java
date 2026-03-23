package com.shixi.ecommerce.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 论坛用户中心内部 Feign 契约。
 *
 * <p>默认地址直接指向论坛用户中心，而不是统一网关。浏览器请求和服务内部请求的入口边界在这里被明确分开。</p>
 */
@FeignClient(
        name = "communityUserInternalClient",
        url = "${community.user-service.base-url}"
)
public interface CommunityUserFeignClient {

    /**
     * 调用论坛登录接口。
     *
     * @param request 登录请求
     * @return 统一响应体
     */
    @PostMapping("/api/auth/login")
    CommunityApiResponse<CommunityAuthUser> login(@RequestBody CommunityAuthRequest request);

    /**
     * 调用论坛注册接口。
     *
     * @param request 注册请求
     * @return 统一响应体
     */
    @PostMapping("/api/auth/register")
    CommunityApiResponse<CommunityAuthUser> register(@RequestBody CommunityAuthRequest request);
}
