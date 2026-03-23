package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.AfterSaleStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 售后状态更新请求。
 *
 * @author shixi
 * @date 2026-03-20
 */
public class AfterSaleUpdateRequest {
    @NotNull
    private AfterSaleStatus status;

    public AfterSaleStatus getStatus() {
        return status;
    }

    public void setStatus(AfterSaleStatus status) {
        this.status = status;
    }
}