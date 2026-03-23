package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.OrderStatus;
import java.util.ArrayList;
import java.util.List;

public class CreateOrderResponse {
    private String orderNo;
    private OrderStatus status;
    private List<String> orderNos = new ArrayList<>();
    private boolean splitByMerchant;

    public CreateOrderResponse() {}

    public CreateOrderResponse(String orderNo, OrderStatus status) {
        this(orderNo, status, orderNo == null ? List.of() : List.of(orderNo), false);
    }

    public CreateOrderResponse(String orderNo, OrderStatus status, List<String> orderNos, boolean splitByMerchant) {
        this.orderNo = orderNo;
        this.status = status;
        setOrderNos(orderNos);
        this.splitByMerchant = splitByMerchant;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<String> getOrderNos() {
        return orderNos;
    }

    public void setOrderNos(List<String> orderNos) {
        this.orderNos = orderNos == null ? new ArrayList<>() : new ArrayList<>(orderNos);
    }

    public boolean isSplitByMerchant() {
        return splitByMerchant;
    }

    public void setSplitByMerchant(boolean splitByMerchant) {
        this.splitByMerchant = splitByMerchant;
    }
}
