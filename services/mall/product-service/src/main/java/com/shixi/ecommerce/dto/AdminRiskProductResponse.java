package com.shixi.ecommerce.dto;

public class AdminRiskProductResponse {
    private Long productId;
    private Long merchantId;
    private String productName;
    private long totalReviews;
    private long negativeReviews;
    private double negativeRatio;
    private String summary;

    public AdminRiskProductResponse(
            Long productId,
            Long merchantId,
            String productName,
            long totalReviews,
            long negativeReviews,
            double negativeRatio,
            String summary) {
        this.productId = productId;
        this.merchantId = merchantId;
        this.productName = productName;
        this.totalReviews = totalReviews;
        this.negativeReviews = negativeReviews;
        this.negativeRatio = negativeRatio;
        this.summary = summary;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getProductName() {
        return productName;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public long getNegativeReviews() {
        return negativeReviews;
    }

    public double getNegativeRatio() {
        return negativeRatio;
    }

    public String getSummary() {
        return summary;
    }
}
