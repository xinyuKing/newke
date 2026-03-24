package com.shixi.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 售后申请请求。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class AfterSaleCreateRequest {
    @NotBlank
    @Size(max = 32)
    private String orderNo;

    private Long skuId;

    @Positive private Integer quantity;

    @NotBlank
    @Size(max = 256)
    private String reason;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
