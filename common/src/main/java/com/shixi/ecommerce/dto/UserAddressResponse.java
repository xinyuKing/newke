package com.shixi.ecommerce.dto;

import java.time.LocalDateTime;

/**
 * 用户地址响应。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class UserAddressResponse {
    private Long id;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private String postalCode;
    private String tag;
    private Boolean isDefault;
    private LocalDateTime createdAt;

    public UserAddressResponse(Long id,
                               Long userId,
                               String receiverName,
                               String receiverPhone,
                               String province,
                               String city,
                               String district,
                               String detailAddress,
                               String postalCode,
                               String tag,
                               Boolean isDefault,
                               LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.province = province;
        this.city = city;
        this.district = district;
        this.detailAddress = detailAddress;
        this.postalCode = postalCode;
        this.tag = tag;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getTag() {
        return tag;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
