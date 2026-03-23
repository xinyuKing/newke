package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.UserAccount;
import com.shixi.ecommerce.dto.UpdateUserProfileRequest;
import com.shixi.ecommerce.dto.UserProfileResponse;
import com.shixi.ecommerce.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户资料服务。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class UserProfileService {
    private final UserAccountRepository repository;

    public UserProfileService(UserAccountRepository repository) {
        this.repository = repository;
    }

    /**
     * 获取用户资料。
     *
     * @param userId 用户 ID
     * @return 用户资料
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        UserAccount account = repository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        return toResponse(account);
    }

    /**
     * 更新用户资料，仅更新请求中提供的字段。
     *
     * @param userId 用户 ID
     * @param request 更新请求
     * @return 更新后的资料
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserAccount account = repository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        if (request.getNickname() != null) {
            account.setNickname(normalize(request.getNickname()));
        }
        if (request.getAvatarUrl() != null) {
            account.setAvatarUrl(normalize(request.getAvatarUrl()));
        }
        if (request.getEmail() != null) {
            account.setEmail(normalize(request.getEmail()));
        }
        if (request.getPhone() != null) {
            account.setPhone(normalize(request.getPhone()));
        }
        repository.save(account);
        return toResponse(account);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private UserProfileResponse toResponse(UserAccount account) {
        return new UserProfileResponse(
                account.getId(),
                account.getUsername(),
                account.getNickname(),
                account.getAvatarUrl(),
                account.getEmail(),
                account.getPhone(),
                account.getCreatedAt());
    }
}
