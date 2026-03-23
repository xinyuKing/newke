package com.shixi.ecommerce.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量库存扣减/释放请求。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class InventoryBatchRequest {
    /** 扣减条目列表。 */
    @NotEmpty
    @Valid
    private List<InventoryDeductRequest> items;

    public List<InventoryDeductRequest> getItems() {
        return items;
    }

    public void setItems(List<InventoryDeductRequest> items) {
        this.items = items;
    }
}
