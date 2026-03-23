package com.shixi.ecommerce.dto;

import java.math.BigDecimal;

/**
 * 订单行数据传输对象，供下单与库存批量接口使用。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class OrderLineItem {
    /** 商品 ID。 */
    private Long skuId;
    /** 购买数量。 */
    private Integer quantity;
    /** 单价（服务端定价后写入）。 */
    private BigDecimal price;

    public OrderLineItem() {}

    public OrderLineItem(Long skuId, Integer quantity, BigDecimal price) {
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
