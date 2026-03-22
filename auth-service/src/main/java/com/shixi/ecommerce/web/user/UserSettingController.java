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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户设置接口，包含个人资料与地址管理。
 *
 * @author shixi
 * @date 2026-03-20
 */
@RestController
@RequestMapping("/api/user")
public class UserSettingController {
    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;
    private final UserAddressService userAddressService;

    public UserSettingController(CurrentUserService currentUserService,
                                 UserProfileService userProfileService,
                                 UserAddressService userAddressService) {
        this.currentUserService = currentUserService;
        this.userProfileService = userProfileService;
        this.userAddressService = userAddressService;
    }

    /**
     * 获取当前用户资料。
     *
     * @return 用户资料
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile() {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userProfileService.getProfile(user.getUserId()));
    }

    /**
     * 更新当前用户资料。
     *
     * @param request 更新请求
     * @return 更新后的资料
     */
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userProfileService.updateProfile(user.getUserId(), request));
    }

    /**
     * 查询当前用户地址列表。
     *
     * @return 地址列表
     */
    @GetMapping("/addresses")
    public ApiResponse<List<UserAddressResponse>> listAddresses() {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userAddressService.list(user.getUserId()));
    }

    /**
     * 新增地址。
     *
     * @param request 地址请求
     * @return 新增地址
     */
    @PostMapping("/addresses")
    public ApiResponse<UserAddressResponse> addAddress(@Valid @RequestBody UserAddressRequest request) {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userAddressService.add(user.getUserId(), request));
    }

    /**
     * 更新地址。
     *
     * @param id      地址 ID
     * @param request 地址请求
     * @return 更新后的地址
     */
    @PutMapping("/addresses/{id}")
    public ApiResponse<UserAddressResponse> updateAddress(@PathVariable Long id,
                                                          @Valid @RequestBody UserAddressRequest request) {
        JwtUser user = currentUserService.getCurrentUser();
        return ApiResponse.ok(userAddressService.update(user.getUserId(), id, request));
    }

    /**
     * 删除地址。
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
     * 设置默认地址。
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
