package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderRefundSnapshotResponse {
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String carrierCode;
    private String trackingNo;
    private LocalDateTime shippedAt;
    private LocalDateTime createdAt;
    private OrderAddressSnapshotResponse shippingAddress;
    private List<OrderItemResponse> items;

    public OrderRefundSnapshotResponse() {}

    public OrderRefundSnapshotResponse(
            String orderNo,
            Long userId,
            OrderStatus status,
            BigDecimal totalAmount,
            String carrierCode,
            String trackingNo,
            LocalDateTime shippedAt,
            LocalDateTime createdAt,
            List<OrderItemResponse> items) {
        this(orderNo, userId, null, status, totalAmount, carrierCode, trackingNo, shippedAt, createdAt, null, items);
    }

    public OrderRefundSnapshotResponse(
            String orderNo,
            Long userId,
            Long merchantId,
            OrderStatus status,
            BigDecimal totalAmount,
            String carrierCode,
            String trackingNo,
            LocalDateTime shippedAt,
            LocalDateTime createdAt,
            List<OrderItemResponse> items) {
        this(
                orderNo,
                userId,
                merchantId,
                status,
                totalAmount,
                carrierCode,
                trackingNo,
                shippedAt,
                createdAt,
                null,
                items);
    }

    public OrderRefundSnapshotResponse(
            String orderNo,
            Long userId,
            Long merchantId,
            OrderStatus status,
            BigDecimal totalAmount,
            String carrierCode,
            String trackingNo,
            LocalDateTime shippedAt,
            LocalDateTime createdAt,
            OrderAddressSnapshotResponse shippingAddress,
            List<OrderItemResponse> items) {
        this.orderNo = orderNo;
        this.userId = userId;
        this.merchantId = merchantId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.carrierCode = carrierCode;
        this.trackingNo = trackingNo;
        this.shippedAt = shippedAt;
        this.createdAt = createdAt;
        this.shippingAddress = shippingAddress;
        this.items = items;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public OrderAddressSnapshotResponse getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(OrderAddressSnapshotResponse shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }
}
