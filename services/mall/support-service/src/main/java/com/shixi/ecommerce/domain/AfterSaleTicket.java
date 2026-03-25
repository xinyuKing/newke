package com.shixi.ecommerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "after_sale_ticket",
        indexes = {
            @Index(name = "idx_after_sale_order_no", columnList = "orderNo"),
            @Index(name = "idx_after_sale_order_sku", columnList = "orderNo,skuId"),
            @Index(name = "idx_after_sale_user", columnList = "userId"),
            @Index(name = "idx_after_sale_user_status", columnList = "userId,status")
        })
public class AfterSaleTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String orderNo;

    @Column(nullable = false)
    private Long userId;

    private Long skuId;

    private Integer quantity;

    @Column(length = 128)
    private String productNameSnapshot;

    @Column(length = 512)
    private String productDescriptionSnapshot;

    @Column(nullable = false, length = 256)
    private String reason;

    @Column(length = 512)
    private String evidenceNote;

    @Lob
    private String evidenceUrlsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AfterSaleStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
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

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public void setProductNameSnapshot(String productNameSnapshot) {
        this.productNameSnapshot = productNameSnapshot;
    }

    public String getProductDescriptionSnapshot() {
        return productDescriptionSnapshot;
    }

    public void setProductDescriptionSnapshot(String productDescriptionSnapshot) {
        this.productDescriptionSnapshot = productDescriptionSnapshot;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidenceNote() {
        return evidenceNote;
    }

    public void setEvidenceNote(String evidenceNote) {
        this.evidenceNote = evidenceNote;
    }

    public String getEvidenceUrlsJson() {
        return evidenceUrlsJson;
    }

    public void setEvidenceUrlsJson(String evidenceUrlsJson) {
        this.evidenceUrlsJson = evidenceUrlsJson;
    }

    public AfterSaleStatus getStatus() {
        return status;
    }

    public void setStatus(AfterSaleStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
