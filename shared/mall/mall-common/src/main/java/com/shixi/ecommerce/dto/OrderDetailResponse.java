package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ∂©µ•œÍ«ÈœÏ”¶°£
 *
 * @author shixi
 * @date 2026-03-20
 */
public class OrderDetailResponse {
    private String orderNo;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String carrierCode;
    private String trackingNo;
    private LocalDateTime shippedAt;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    public OrderDetailResponse(String orderNo,
                               OrderStatus status,
                               BigDecimal totalAmount,
                               String carrierCode,
                               String trackingNo,
                               LocalDateTime shippedAt,
                               LocalDateTime createdAt,
                               List<OrderItemResponse> items) {
        this.orderNo = orderNo;
        this.status = status;
        this.totalAmount = totalAmount;
        this.carrierCode = carrierCode;
        this.trackingNo = trackingNo;
        this.shippedAt = shippedAt;
        this.createdAt = createdAt;
        this.items = items;
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

    public String getCarrierCode() {
        return carrierCode;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }
}