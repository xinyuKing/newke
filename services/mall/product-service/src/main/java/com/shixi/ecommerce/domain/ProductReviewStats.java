package com.shixi.ecommerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 商品评价统计快照，供分析与推荐使用。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Entity
@Table(name = "product_review_stats")
public class ProductReviewStats {
    @Id
    private Long productId;

    @Column(nullable = false)
    private Long totalReviews;

    @Column(nullable = false)
    private Long negativeReviews;

    @Column(nullable = false)
    private Double avgRating;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        if (totalReviews == null) {
            totalReviews = 0L;
        }
        if (negativeReviews == null) {
            negativeReviews = 0L;
        }
        if (avgRating == null) {
            avgRating = 0.0;
        }
        updatedAt = LocalDateTime.now();
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Long getNegativeReviews() {
        return negativeReviews;
    }

    public void setNegativeReviews(Long negativeReviews) {
        this.negativeReviews = negativeReviews;
    }

    public Double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(Double avgRating) {
        this.avgRating = avgRating;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
