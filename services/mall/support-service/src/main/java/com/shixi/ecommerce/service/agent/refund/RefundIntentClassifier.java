package com.shixi.ecommerce.service.agent.refund;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shixi.ecommerce.config.RefundModelProperties;
import com.shixi.ecommerce.domain.RefundIntentModelState;
import com.shixi.ecommerce.dto.RefundIntentSample;
import com.shixi.ecommerce.repository.RefundIntentModelStateRepository;
import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RefundIntentClassifier {
    private final RefundTokenizer tokenizer;
    private final RefundIntentModelStateRepository repository;
    private final ObjectMapper objectMapper;
    private final RefundModelProperties properties;
    private final RefundIntentModel model = new RefundIntentModel();
    private final Set<String> keywordSet = new HashSet<>();
    private RefundIntentAlgorithm algorithm = RefundIntentAlgorithm.NAIVE_BAYES;
    private double threshold = 0.60;
    private boolean trained;

    public RefundIntentClassifier(
            RefundTokenizer tokenizer,
            RefundIntentModelStateRepository repository,
            ObjectMapper objectMapper,
            RefundModelProperties properties) {
        this.tokenizer = tokenizer;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        loadFromStore();
        if (!trained) {
            String algorithmName = properties.getIntent().getAlgorithm();
            RefundIntentAlgorithm configured = parseAlgorithm(algorithmName, RefundIntentAlgorithm.NAIVE_BAYES);
            threshold = properties.getIntent().getThreshold();
            train(configured, RefundIntentSample.defaultSamples());
            persist(configured, threshold);
        }
    }

    public boolean isRefund(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        for (String keyword : keywordSet) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        if (!trained) {
            return false;
        }
        double probability = model.predictProbability(tokenizer.tokenize(lower));
        return probability >= threshold;
    }

    public synchronized void train(RefundIntentAlgorithm algorithm, List<RefundIntentSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return;
        }
        this.algorithm = algorithm;
        keywordSet.clear();
        model.reset();
        for (RefundIntentSample sample : samples) {
            if (sample == null || sample.getText() == null) {
                continue;
            }
            boolean refund = sample.getLabel() == RefundIntentLabel.REFUND;
            List<String> tokens = tokenizer.tokenize(sample.getText().toLowerCase(Locale.ROOT));
            if (algorithm == RefundIntentAlgorithm.KEYWORD && refund) {
                keywordSet.addAll(tokens);
            } else {
                model.update(refund, tokens);
            }
        }
        if (algorithm == RefundIntentAlgorithm.NAIVE_BAYES) {
            keywordSet.add("\u9000\u6b3e");
            keywordSet.add("\u9000\u8d27");
            keywordSet.add("refund");
        }
        trained = true;
    }

    public RefundIntentAlgorithm getAlgorithm() {
        return algorithm;
    }

    public double getThreshold() {
        return threshold;
    }

    public void updateThreshold(double threshold) {
        this.threshold = threshold;
    }

    private RefundIntentAlgorithm parseAlgorithm(String value, RefundIntentAlgorithm fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return RefundIntentAlgorithm.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private void persist(RefundIntentAlgorithm algorithm, double threshold) {
        RefundIntentModelState state = new RefundIntentModelState();
        state.setModelVersion(algorithm.name());
        try {
            Map<String, Object> payload = model.snapshot();
            payload.put("algorithm", algorithm.name());
            payload.put("threshold", threshold);
            payload.put("keywords", keywordSet);
            String json = objectMapper.writeValueAsString(payload);
            state.setPayload(json);
            repository.save(state);
        } catch (JsonProcessingException ex) {
            // ignore persistence failure
        }
        this.algorithm = algorithm;
        this.threshold = threshold;
    }

    public void persistCurrent() {
        if (algorithm == null) {
            return;
        }
        persist(algorithm, threshold);
    }

    private void loadFromStore() {
        String algorithmName = properties.getIntent().getAlgorithm();
        RefundIntentAlgorithm configured = parseAlgorithm(algorithmName, RefundIntentAlgorithm.NAIVE_BAYES);
        threshold = properties.getIntent().getThreshold();
        repository.findTopByModelVersionOrderByUpdatedAtDesc(configured.name()).ifPresent(state -> {
            try {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> payload = objectMapper.readValue(state.getPayload(), java.util.Map.class);
                model.restore(payload);
                Object keywords = payload.get("keywords");
                if (keywords instanceof Iterable<?> iterable) {
                    for (Object item : iterable) {
                        if (item != null) {
                            keywordSet.add(String.valueOf(item));
                        }
                    }
                }
                Object algo = payload.get("algorithm");
                if (algo != null) {
                    algorithm = parseAlgorithm(String.valueOf(algo), configured);
                } else {
                    algorithm = configured;
                }
                Object th = payload.get("threshold");
                if (th != null) {
                    try {
                        threshold = Double.parseDouble(String.valueOf(th));
                    } catch (NumberFormatException ignored) {
                        threshold = properties.getIntent().getThreshold();
                    }
                }
                keywordSet.add("\u9000\u6b3e");
                keywordSet.add("\u9000\u8d27");
                keywordSet.add("refund");
                trained = true;
            } catch (Exception ignored) {
                trained = false;
            }
        });
    }
}
