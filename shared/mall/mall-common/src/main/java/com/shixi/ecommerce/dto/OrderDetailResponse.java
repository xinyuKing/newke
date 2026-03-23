package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order detail response.
 *
 * @author shixi
 * @date 2026-03-20
 */
public class OrderDetailResponse {
    private String orderNo;
    private Long merchantId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String carrierCode;
    private String trackingNo;
    private LocalDateTime shippedAt;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    public OrderDetailResponse() {}

    public OrderDetailResponse(
            String orderNo,
            OrderStatus status,
            BigDecimal totalAmount,
            String carrierCode,
            String trackingNo,
            LocalDateTime shippedAt,
            LocalDateTime createdAt,
            List<OrderItemResponse> items) {
        this(orderNo, null, status, totalAmount, carrierCode, trackingNo, shippedAt, createdAt, items);
    }

    public OrderDetailResponse(
            String orderNo,
            Long merchantId,
            OrderStatus status,
            BigDecimal totalAmount,
            String carrierCode,
            String trackingNo,
            LocalDateTime shippedAt,
            LocalDateTime createdAt,
            List<OrderItemResponse> items) {
        this.orderNo = orderNo;
        this.merchantId = merchantId;
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

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }
}
