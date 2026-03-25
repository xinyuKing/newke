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
    private String productName;
    private String productDescription;

    public OrderItemResponse() {}

    public OrderItemResponse(Long skuId, Integer quantity, BigDecimal price) {
        this(skuId, quantity, price, null, null);
    }

    public OrderItemResponse(
            Long skuId, Integer quantity, BigDecimal price, String productName, String productDescription) {
        this.skuId = skuId;
        this.quantity = quantity;
        this.price = price;
        this.productName = productName;
        this.productDescription = productDescription;
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }
}
