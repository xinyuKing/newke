package com.shixi.ecommerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "checkout_record",
        indexes = {
            @Index(name = "idx_checkout_record_biz_key", columnList = "bizKey", unique = true),
            @Index(name = "idx_checkout_record_user", columnList = "userId")
        })
public class CheckoutRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128, unique = true)
    private String bizKey;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String idempotencyKey;

    @Lob
    @Column(nullable = false)
    private String checkoutItemsJson;

    @Column(nullable = false)
    private boolean cartCleared;

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

    public String getBizKey() {
        return bizKey;
    }

    public void setBizKey(String bizKey) {
        this.bizKey = bizKey;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCheckoutItemsJson() {
        return checkoutItemsJson;
    }

    public void setCheckoutItemsJson(String checkoutItemsJson) {
        this.checkoutItemsJson = checkoutItemsJson;
    }

    public boolean isCartCleared() {
        return cartCleared;
    }

    public void setCartCleared(boolean cartCleared) {
        this.cartCleared = cartCleared;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
