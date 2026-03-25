package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.AfterSaleStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后响应。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class AfterSaleResponse {
    private Long id;
    private String orderNo;
    private Long skuId;
    private Integer quantity;
    private String productName;
    private String productDescription;
    private String reason;
    private String evidenceNote;
    private List<String> evidenceUrls;
    private AfterSaleStatus status;
    private LocalDateTime createdAt;

    public AfterSaleResponse(Long id, String orderNo, String reason, AfterSaleStatus status, LocalDateTime createdAt) {
        this(id, orderNo, null, null, null, null, reason, null, List.of(), status, createdAt);
    }

    public AfterSaleResponse(
            Long id,
            String orderNo,
            Long skuId,
            Integer quantity,
            String productName,
            String productDescription,
            String reason,
            String evidenceNote,
            List<String> evidenceUrls,
            AfterSaleStatus status,
            LocalDateTime createdAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.skuId = skuId;
        this.quantity = quantity;
        this.productName = productName;
        this.productDescription = productDescription;
        this.reason = reason;
        this.evidenceNote = evidenceNote;
        this.evidenceUrls = evidenceUrls == null ? List.of() : List.copyOf(evidenceUrls);
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getSkuId() {
        return skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public String getReason() {
        return reason;
    }

    public String getEvidenceNote() {
        return evidenceNote;
    }

    public List<String> getEvidenceUrls() {
        return evidenceUrls;
    }

    public boolean isEvidenceProvided() {
        return (evidenceNote != null && !evidenceNote.isBlank()) || (evidenceUrls != null && !evidenceUrls.isEmpty());
    }

    public AfterSaleStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
