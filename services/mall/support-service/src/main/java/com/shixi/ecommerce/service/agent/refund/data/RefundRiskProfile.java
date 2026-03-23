package com.shixi.ecommerce.service.agent.refund.data;

public record RefundRiskProfile(
        long totalAfterSaleCount,
        long recentAfterSaleCount,
        long openAfterSaleCount,
        boolean existingAfterSaleTicket) {}
