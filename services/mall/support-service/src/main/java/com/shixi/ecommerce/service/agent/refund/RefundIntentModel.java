package com.shixi.ecommerce.service.agent.refund;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class RefundIntentModel {
    private final Map<String, Integer> refundCounts = new HashMap<>();
    private final Map<String, Integer> nonRefundCounts = new HashMap<>();
    private int refundDocs;
    private int nonRefundDocs;
    private int refundWords;
    private int nonRefundWords;

    void reset() {
        refundCounts.clear();
        nonRefundCounts.clear();
        refundDocs = 0;
        nonRefundDocs = 0;
        refundWords = 0;
        nonRefundWords = 0;
    }

    void update(boolean refund, List<String> tokens) {
        if (refund) {
            refundDocs++;
            for (String token : tokens) {
                refundCounts.put(token, refundCounts.getOrDefault(token, 0) + 1);
                refundWords++;
            }
        } else {
            nonRefundDocs++;
            for (String token : tokens) {
                nonRefundCounts.put(token, nonRefundCounts.getOrDefault(token, 0) + 1);
                nonRefundWords++;
            }
        }
    }

    double predictProbability(List<String> tokens) {
        int totalDocs = refundDocs + nonRefundDocs;
        if (totalDocs == 0) {
            return 0.0;
        }
        Set<String> vocab = buildVocabulary();
        int vocabSize = vocab.size();
        if (vocabSize == 0) {
            return 0.0;
        }

        double logRefund = Math.log((double) refundDocs / totalDocs);
        double logNonRefund = Math.log((double) nonRefundDocs / totalDocs);

        for (String token : tokens) {
            int refundCount = refundCounts.getOrDefault(token, 0);
            int nonRefundCount = nonRefundCounts.getOrDefault(token, 0);
            logRefund += Math.log((refundCount + 1.0) / (refundWords + vocabSize));
            logNonRefund += Math.log((nonRefundCount + 1.0) / (nonRefundWords + vocabSize));
        }
        double odds = Math.exp(logRefund - logNonRefund);
        return odds / (1.0 + odds);
    }

    private Set<String> buildVocabulary() {
        Set<String> vocab = refundCounts.keySet().stream().collect(Collectors.toSet());
        vocab.addAll(nonRefundCounts.keySet());
        return vocab;
    }

    Map<String, Object> snapshot() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("refundCounts", new HashMap<>(refundCounts));
        payload.put("nonRefundCounts", new HashMap<>(nonRefundCounts));
        payload.put("refundDocs", refundDocs);
        payload.put("nonRefundDocs", nonRefundDocs);
        payload.put("refundWords", refundWords);
        payload.put("nonRefundWords", nonRefundWords);
        return payload;
    }

    @SuppressWarnings("unchecked")
    void restore(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        refundCounts.clear();
        nonRefundCounts.clear();
        Object refundMap = payload.get("refundCounts");
        if (refundMap instanceof Map<?, ?> map) {
            map.forEach((k, v) -> refundCounts.put(String.valueOf(k), Integer.parseInt(String.valueOf(v))));
        }
        Object nonRefundMap = payload.get("nonRefundCounts");
        if (nonRefundMap instanceof Map<?, ?> map) {
            map.forEach((k, v) -> nonRefundCounts.put(String.valueOf(k), Integer.parseInt(String.valueOf(v))));
        }
        refundDocs = parseInt(payload.get("refundDocs"));
        nonRefundDocs = parseInt(payload.get("nonRefundDocs"));
        refundWords = parseInt(payload.get("refundWords"));
        nonRefundWords = parseInt(payload.get("nonRefundWords"));
    }

    private int parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
