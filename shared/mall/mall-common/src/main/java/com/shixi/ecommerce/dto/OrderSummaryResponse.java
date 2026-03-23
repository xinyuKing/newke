package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order summary response.
 *
 * @author shixi
 * @date 2026-03-20
 */
public class OrderSummaryResponse {
    private String orderNo;
    private Long merchantId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    public OrderSummaryResponse() {}

    public OrderSummaryResponse(String orderNo, OrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt) {
        this(orderNo, null, status, totalAmount, createdAt);
    }

    public OrderSummaryResponse(
            String orderNo, Long merchantId, OrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt) {
        this.orderNo = orderNo;
        this.merchantId = merchantId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
