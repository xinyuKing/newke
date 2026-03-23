package com.shixi.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户地址请求。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class UserAddressRequest {
    /** 收件人姓名。 */
    @NotBlank
    @Size(max = 32)
    private String receiverName;

    /** 收件人电话。 */
    @NotBlank
    @Size(max = 32)
    private String receiverPhone;

    /** 省份。 */
    @NotBlank
    @Size(max = 64)
    private String province;

    /** 城市。 */
    @NotBlank
    @Size(max = 64)
    private String city;

    /** 区县。 */
    @Size(max = 64)
    private String district;

    /** 详细地址。 */
    @NotBlank
    @Size(max = 256)
    private String detailAddress;

    /** 邮编。 */
    @Size(max = 16)
    private String postalCode;

    /** 标签（如家/公司）。 */
    @Size(max = 32)
    private String tag;

    /** 是否默认地址。 */
    private Boolean isDefault;

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getDetailAddress() {
        return detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
