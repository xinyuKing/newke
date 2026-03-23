package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.OrderStatus;

public class CreateOrderResponse {
    private String orderNo;
    private OrderStatus status;

    public CreateOrderResponse(String orderNo, OrderStatus status) {
        this.orderNo = orderNo;
        this.status = status;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
