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
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    public OrderSummaryResponse(String orderNo, OrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt) {
        this.orderNo = orderNo;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
