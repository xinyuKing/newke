package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.AfterSaleStatus;
import java.time.LocalDateTime;

/**
 * 售后响应。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class AfterSaleResponse {
    private Long id;
    private String orderNo;
    private String reason;
    private AfterSaleStatus status;
    private LocalDateTime createdAt;

    public AfterSaleResponse(Long id, String orderNo, String reason, AfterSaleStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getReason() {
        return reason;
    }

    public AfterSaleStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
