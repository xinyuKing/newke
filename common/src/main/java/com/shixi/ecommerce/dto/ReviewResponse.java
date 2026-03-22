package com.shixi.ecommerce.dto;

import java.time.LocalDateTime;

/**
 * 评价响应。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class ReviewResponse {
    private Long id;
    private Long productId;
    private Long userId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;

    public ReviewResponse(Long id,
                          Long productId,
                          Long userId,
                          Integer rating,
                          String content,
                          LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}