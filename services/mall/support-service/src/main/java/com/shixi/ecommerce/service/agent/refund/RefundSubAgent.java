package com.shixi.ecommerce.service.agent.refund;

public interface RefundSubAgent {
    String getType();

    RefundAgentOutput handle(RefundContext context);
}
