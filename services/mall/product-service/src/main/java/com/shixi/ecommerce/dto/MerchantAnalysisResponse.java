package com.shixi.ecommerce.dto;

import java.util.List;

public class MerchantAnalysisResponse {
    private Long merchantId;
    private long productCount;
    private long activeProductCount;
    private long totalReviews;
    private double avgRating;
    private List<String> riskProducts;
    private String summary;

    public MerchantAnalysisResponse(
            Long merchantId,
            long productCount,
            long activeProductCount,
            long totalReviews,
            double avgRating,
            List<String> riskProducts,
            String summary) {
        this.merchantId = merchantId;
        this.productCount = productCount;
        this.activeProductCount = activeProductCount;
        this.totalReviews = totalReviews;
        this.avgRating = avgRating;
        this.riskProducts = riskProducts;
        this.summary = summary;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public long getProductCount() {
        return productCount;
    }

    public long getActiveProductCount() {
        return activeProductCount;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public List<String> getRiskProducts() {
        return riskProducts;
    }

    public String getSummary() {
        return summary;
    }
}
