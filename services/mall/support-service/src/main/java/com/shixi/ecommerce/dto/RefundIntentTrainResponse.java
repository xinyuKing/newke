package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.service.agent.refund.RefundIntentAlgorithm;

public class RefundIntentTrainResponse {
    private RefundIntentAlgorithm algorithm;
    private int total;
    private int refundCount;
    private int nonRefundCount;
    private double threshold;

    public RefundIntentTrainResponse(RefundIntentAlgorithm algorithm, int total, int refundCount, int nonRefundCount) {
        this.algorithm = algorithm;
        this.total = total;
        this.refundCount = refundCount;
        this.nonRefundCount = nonRefundCount;
    }

    public RefundIntentTrainResponse(
            RefundIntentAlgorithm algorithm, int total, int refundCount, int nonRefundCount, double threshold) {
        this.algorithm = algorithm;
        this.total = total;
        this.refundCount = refundCount;
        this.nonRefundCount = nonRefundCount;
        this.threshold = threshold;
    }

    public RefundIntentAlgorithm getAlgorithm() {
        return algorithm;
    }

    public int getTotal() {
        return total;
    }

    public int getRefundCount() {
        return refundCount;
    }

    public int getNonRefundCount() {
        return nonRefundCount;
    }

    public double getThreshold() {
        return threshold;
    }
}
