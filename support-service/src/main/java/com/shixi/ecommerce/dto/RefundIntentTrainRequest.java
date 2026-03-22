package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.service.agent.refund.RefundIntentAlgorithm;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class RefundIntentTrainRequest {
    private RefundIntentAlgorithm algorithm = RefundIntentAlgorithm.NAIVE_BAYES;

    @NotEmpty
    private List<RefundIntentSample> samples;

    private Double threshold;

    public RefundIntentAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(RefundIntentAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public List<RefundIntentSample> getSamples() {
        return samples;
    }

    public void setSamples(List<RefundIntentSample> samples) {
        this.samples = samples;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }
}
