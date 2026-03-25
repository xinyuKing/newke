package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.ProductStatus;
import java.math.BigDecimal;

public class CartItemResponse {
    private Long skuId;
    private Integer quantity;
    private BigDecimal priceSnapshot;
    private String productName;
    private String productDescription;
    private ProductStatus productStatus;
    private Boolean productAvailable;

    public CartItemResponse() {}

    public CartItemResponse(Long skuId, Integer quantity, BigDecimal priceSnapshot) {
        this(skuId, quantity, priceSnapshot, null, null, null, null);
    }

    public CartItemResponse(
            Long skuId, Integer quantity, BigDecimal priceSnapshot, String productName, String productDescription) {
        this(skuId, quantity, priceSnapshot, productName, productDescription, null, null);
    }

    public CartItemResponse(
            Long skuId,
            Integer quantity,
            BigDecimal priceSnapshot,
            String productName,
            String productDescription,
            ProductStatus productStatus,
            Boolean productAvailable) {
        this.skuId = skuId;
        this.quantity = quantity;
        this.priceSnapshot = priceSnapshot;
        this.productName = productName;
        this.productDescription = productDescription;
        this.productStatus = productStatus;
        this.productAvailable = productAvailable;
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

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(BigDecimal priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
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

    public ProductStatus getProductStatus() {
        return productStatus;
    }

    public void setProductStatus(ProductStatus productStatus) {
        this.productStatus = productStatus;
    }

    public Boolean getProductAvailable() {
        return productAvailable;
    }

    public void setProductAvailable(Boolean productAvailable) {
        this.productAvailable = productAvailable;
    }
}
