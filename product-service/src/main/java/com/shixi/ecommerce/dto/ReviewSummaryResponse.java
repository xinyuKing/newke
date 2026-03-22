package com.shixi.ecommerce.dto;

public class ReviewSummaryResponse {
    private Long productId;
    private Long reviewCount;
    private String summary;

    public ReviewSummaryResponse(Long productId, Long reviewCount, String summary) {
        this.productId = productId;
        this.reviewCount = reviewCount;
        this.summary = summary;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getReviewCount() {
        return reviewCount;
    }

    public String getSummary() {
        return summary;
    }
}
