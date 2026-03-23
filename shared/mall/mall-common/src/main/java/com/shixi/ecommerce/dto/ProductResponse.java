package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.ProductStatus;
import java.math.BigDecimal;

public class ProductResponse {
    private Long id;
    private Long merchantId;
    private String name;
    private String description;
    private String videoUrl;
    private BigDecimal price;
    private ProductStatus status;

    public ProductResponse() {}

    public ProductResponse(
            Long id,
            Long merchantId,
            String name,
            String description,
            String videoUrl,
            BigDecimal price,
            ProductStatus status) {
        this.id = id;
        this.merchantId = merchantId;
        this.name = name;
        this.description = description;
        this.videoUrl = videoUrl;
        this.price = price;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }
}
