package com.shixi.ecommerce.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotent_record", indexes = {
        @Index(name = "idx_idempotent_biz", columnList = "bizKey", unique = true)
})
public class IdempotentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String bizKey;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
