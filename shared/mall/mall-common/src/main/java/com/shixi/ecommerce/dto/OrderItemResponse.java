package com.shixi.ecommerce.dto;

import java.math.BigDecimal;

/**
 * Order item response.
 *
 * @author shixi
 * @date 2026-03-20
 */
public class OrderItemResponse {
    private Long skuId;
    private Integer quantity;
    private BigDecimal price;

    public OrderItemResponse() {}

    public OrderItemResponse(Long skuId, Integer quantity, BigDecimal price) {
        this.skuId = skuId;
        this.quantity = quantity;
        this.price = price;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
