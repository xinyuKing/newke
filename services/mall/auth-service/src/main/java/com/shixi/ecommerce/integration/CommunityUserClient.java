package com.shixi.ecommerce.integration;

import com.shixi.ecommerce.common.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 论坛用户中心客户端封装。
 *
 * <p>商城认证服务不会通过公网网关反向调用论坛，而是直接调用论坛用户中心的内部接口。
 * 这样既能复用论坛主账号体系，又能保持浏览器入口和服务入口边界清晰。</p>
 */
@Component
public class CommunityUserClient {

    private static final int SUCCESS_CODE = 0;
    private static final String LOGIN_ERROR_MESSAGE = "论坛用户中心登录失败";
    private static final String REGISTER_ERROR_MESSAGE = "论坛用户中心注册失败";
    private static final String EMPTY_RESPONSE_MESSAGE = "论坛用户中心返回结果为空";
    private static final String EMPTY_PAYLOAD_MESSAGE = "论坛用户中心返回数据为空";

    private final CommunityUserFeignClient feignClient;

    public CommunityUserClient(CommunityUserFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    /**
     * 调用论坛用户中心完成登录校验。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 论坛用户中心返回的用户信息
     */
    public CommunityAuthUser login(String username, String password) {
        CommunityAuthRequest request = new CommunityAuthRequest(username, password, null, true);
        return unwrap(feignClient.login(request), LOGIN_ERROR_MESSAGE);
    }

    /**
     * 调用论坛用户中心完成账号注册。
     *
     * @param username 用户名
     * @param password 明文密码
     * @param email    邮箱，可为空
     * @return 论坛用户中心返回的用户信息
     */
    public CommunityAuthUser register(String username, String password, String email) {
        CommunityAuthRequest request = new CommunityAuthRequest(username, password, email, true);
        return unwrap(feignClient.register(request), REGISTER_ERROR_MESSAGE);
    }

    /**
     * 统一校验论坛用户中心的响应结构。
     *
     * @param response       Feign 调用返回结果
     * @param defaultMessage 默认异常信息
     * @return 业务数据体
     */
    private CommunityAuthUser unwrap(CommunityApiResponse<CommunityAuthUser> response, String defaultMessage) {
        if (response == null) {
            throw new BusinessException(EMPTY_RESPONSE_MESSAGE);
        }
        if (response.getCode() != SUCCESS_CODE) {
            throw new BusinessException(response.getMsg() == null ? defaultMessage : response.getMsg());
        }
        if (response.getData() == null) {
            throw new BusinessException(EMPTY_PAYLOAD_MESSAGE);
        }
        return response.getData();
    }
}
