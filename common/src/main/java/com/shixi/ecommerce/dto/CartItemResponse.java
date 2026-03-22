package com.shixi.ecommerce.dto;

import java.math.BigDecimal;

public class CartItemResponse {
    private Long skuId;
    private Integer quantity;
    private BigDecimal priceSnapshot;

    public CartItemResponse(Long skuId, Integer quantity, BigDecimal priceSnapshot) {
        this.skuId = skuId;
        this.quantity = quantity;
        this.priceSnapshot = priceSnapshot;
    }

    public Long getSkuId() {
        return skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }
}
