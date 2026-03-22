package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.dto.RefundIntentSample;
import com.shixi.ecommerce.dto.RefundIntentTrainRequest;
import com.shixi.ecommerce.dto.RefundIntentTrainResponse;
import org.springframework.stereotype.Service;

@Service
public class RefundIntentTrainingService {
    private final RefundIntentClassifier classifier;

    public RefundIntentTrainingService(RefundIntentClassifier classifier) {
        this.classifier = classifier;
    }

    public RefundIntentTrainResponse train(RefundIntentTrainRequest request) {
        if (request == null || request.getSamples() == null) {
            return new RefundIntentTrainResponse(RefundIntentAlgorithm.NAIVE_BAYES, 0, 0, 0, 0.0);
        }
        RefundIntentAlgorithm algorithm = request.getAlgorithm() == null
                ? RefundIntentAlgorithm.NAIVE_BAYES
                : request.getAlgorithm();
        int refundCount = 0;
        int nonRefundCount = 0;
        for (RefundIntentSample sample : request.getSamples()) {
            if (sample != null && sample.getLabel() == RefundIntentLabel.REFUND) {
                refundCount++;
            } else {
                nonRefundCount++;
            }
        }
        classifier.train(algorithm, request.getSamples());
        double threshold = request.getThreshold() == null ? classifier.getThreshold() : request.getThreshold();
        classifier.updateThreshold(threshold);
        classifier.persistCurrent();
        return new RefundIntentTrainResponse(algorithm, request.getSamples().size(), refundCount, nonRefundCount, threshold);
    }
}
