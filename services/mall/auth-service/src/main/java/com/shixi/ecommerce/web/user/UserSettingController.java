package com.shixi.ecommerce.web.user;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.UpdateUserProfileRequest;
import com.shixi.ecommerce.dto.UserAddressRequest;
import com.shixi.ecommerce.dto.UserAddressResponse;
import com.shixi.ecommerce.dto.UserProfileResponse;
import com.shixi.ecommerce.security.JwtUser;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.UserAddressService;
import com.shixi.ecommerce.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城用户资料接口。
 *
 * <p>该控制器负责商城侧的扩展资料维护，包括个人信息和收货地址，不承担论坛主账号的注册与登录职责。</p>
 */
@RestController
@RequestMapping("/api/user")
public class UserSettingController {

    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;
    private final UserAddressService userAddressService;

    public UserSettingController(
            CurrentUserService currentUserService,
            UserProfileService userProfileService,
            UserAddressService userAddressService) {
        this.currentUserService = currentUserService;
        this.userProfileService = userProfileService;
        this.userAddressService = userAddressService;
    }

    /**
     * 查询当前登录用户资料。
     *
     * @return 用户资料
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile() {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userProfileService.getProfile(user.getUserId()));
    }

    /**
     * 更新当前登录用户资料。
     *
     * @param request 资料更新请求
     * @return 更新后的用户资料
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userProfileService.updateProfile(user.getUserId(), request));
    }

    /**
     * 查询当前登录用户的收货地址列表。
     *
     * @return 地址列表
     */
    @GetMapping("/addresses")
    public ApiResponse<List<UserAddressResponse>> listAddresses() {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userAddressService.list(user.getUserId()));
    }

    /**
     * 新增收货地址。
     *
     * @param request 地址请求参数
     * @return 新增后的地址信息
     */
    @PostMapping("/addresses")
    public ApiResponse<UserAddressResponse> addAddress(@Valid @RequestBody UserAddressRequest request) {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userAddressService.add(user.getUserId(), request));
    }

    /**
     * 更新收货地址。
     *
     * @param id      地址 ID
     * @param request 地址请求参数
     * @return 更新后的地址信息
     */
    @PutMapping("/addresses/{id}")
    public ApiResponse<UserAddressResponse> updateAddress(
            @PathVariable Long id, @Valid @RequestBody UserAddressRequest request) {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userAddressService.update(user.getUserId(), id, request));
    }

    /**
     * 删除收货地址。
     *
     * @param id 地址 ID
     * @return 删除结果
     */
    @DeleteMapping("/addresses/{id}")
    public ApiResponse<String> deleteAddress(@PathVariable Long id) {
        JwtUser user = currentUserService.getCurrentUser();
        userAddressService.delete(user.getUserId(), id);
        return ApiResponse.ok("OK");
    }

    /**
     * 设置默认收货地址。
     *
     * @param id 地址 ID
     * @return 设置结果
     */
    @PutMapping("/addresses/{id}/default")
    public ApiResponse<String> setDefault(@PathVariable Long id) {
        JwtUser user = currentUserService.getCurrentUser();
        userAddressService.setDefault(user.getUserId(), id);
        return ApiResponse.ok("OK");
    }
}
