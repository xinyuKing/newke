package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.UserAddress;
import com.shixi.ecommerce.dto.UserAddressRequest;
import com.shixi.ecommerce.dto.UserAddressResponse;
import com.shixi.ecommerce.repository.UserAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户地址服务。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class UserAddressService {
    private final UserAddressRepository repository;

    public UserAddressService(UserAddressRepository repository) {
        this.repository = repository;
    }

    /**
     * 查询用户地址列表。
     *
     * @param userId 用户 ID
     * @return 地址列表
     */
    @Transactional(readOnly = true)
    public List<UserAddressResponse> list(Long userId) {
        return repository.findByUserIdOrderByIsDefaultDescIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 新增用户地址。
     *
     * @param userId 用户 ID
     * @param request 地址请求
     * @return 地址响应
     */
    @Transactional
    public UserAddressResponse add(Long userId, UserAddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        apply(address, request);
        boolean shouldDefault = Boolean.TRUE.equals(request.getIsDefault()) || repository.countByUserId(userId) == 0;
        if (shouldDefault) {
            repository.clearDefault(userId);
            address.setDefault(true);
        }
        repository.save(address);
        return toResponse(address);
    }

    /**
     * 更新用户地址。
     *
     * @param userId 用户 ID
     * @param id 地址 ID
     * @param request 地址请求
     * @return 地址响应
     */
    @Transactional
    public UserAddressResponse update(Long userId, Long id, UserAddressRequest request) {
        UserAddress address = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("Address not found"));
        apply(address, request);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            repository.clearDefault(userId);
            address.setDefault(true);
        } else if (request.getIsDefault() != null) {
            address.setDefault(false);
        }
        repository.save(address);
        return toResponse(address);
    }

    /**
     * 删除用户地址。
     *
     * @param userId 用户 ID
     * @param id 地址 ID
     */
    @Transactional
    public void delete(Long userId, Long id) {
        UserAddress address = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("Address not found"));
        boolean wasDefault = address.isDefault();
        repository.delete(address);
        if (wasDefault) {
            List<UserAddress> remaining = repository.findByUserIdOrderByIsDefaultDescIdDesc(userId);
            if (!remaining.isEmpty()) {
                repository.clearDefault(userId);
                UserAddress first = remaining.get(0);
                first.setDefault(true);
                repository.save(first);
            }
        }
    }

    /**
     * 设置默认地址。
     *
     * @param userId 用户 ID
     * @param id 地址 ID
     */
    @Transactional
    public void setDefault(Long userId, Long id) {
        UserAddress address = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("Address not found"));
        repository.clearDefault(userId);
        address.setDefault(true);
        repository.save(address);
    }

    private void apply(UserAddress address, UserAddressRequest request) {
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        address.setPostalCode(request.getPostalCode());
        address.setTag(request.getTag());
        if (request.getIsDefault() != null) {
            address.setDefault(request.getIsDefault());
        }
    }

    private UserAddressResponse toResponse(UserAddress address) {
        return new UserAddressResponse(
                address.getId(),
                address.getUserId(),
                address.getReceiverName(),
                address.getReceiverPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress(),
                address.getPostalCode(),
                address.getTag(),
                address.isDefault(),
                address.getCreatedAt()
        );
    }
}