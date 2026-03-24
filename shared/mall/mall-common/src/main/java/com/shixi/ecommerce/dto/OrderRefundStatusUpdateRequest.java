package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class OrderRefundStatusUpdateRequest {
    @NotNull private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
