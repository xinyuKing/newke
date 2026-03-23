package com.shixi.ecommerce.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量商品查询请求。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class ProductBatchRequest {
    /** 商品 ID 列表。 */
    @NotEmpty
    private List<Long> skuIds;

    public List<Long> getSkuIds() {
        return skuIds;
    }

    public void setSkuIds(List<Long> skuIds) {
        this.skuIds = skuIds;
    }
}
