package com.shixi.ecommerce.dto;

import java.math.BigDecimal;

/**
 * Order line item DTO shared by order creation and inventory batch APIs.
 *
 * @author shixi
 * @date 2026-03-20
 */
public class OrderLineItem {
    /** Product sku id. */
    private Long skuId;
    /** Purchase quantity. */
    private Integer quantity;
    /** Unit price after server-side pricing. */
    private BigDecimal price;

    private Long merchantId;

    private String productName;

    private String productDescription;

    public OrderLineItem() {}

    public OrderLineItem(Long skuId, Integer quantity, BigDecimal price) {
        this(skuId, quantity, price, null, null, null);
    }

    public OrderLineItem(Long skuId, Integer quantity, BigDecimal price, Long merchantId) {
        this(skuId, quantity, price, merchantId, null, null);
    }

    public OrderLineItem(
            Long skuId,
            Integer quantity,
            BigDecimal price,
            Long merchantId,
            String productName,
            String productDescription) {
        this.skuId = skuId;
        this.quantity = quantity;
        this.price = price;
        this.merchantId = merchantId;
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

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
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
