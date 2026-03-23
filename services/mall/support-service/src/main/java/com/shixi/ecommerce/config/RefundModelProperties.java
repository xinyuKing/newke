package com.shixi.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "refund")
public class RefundModelProperties {
    private RefundIntentProperties intent = new RefundIntentProperties();
    private RefundRagProperties rag = new RefundRagProperties();
    private RefundModelRegistryProperties model = new RefundModelRegistryProperties();

    public RefundIntentProperties getIntent() {
        return intent;
    }

    public void setIntent(RefundIntentProperties intent) {
        this.intent = intent;
    }

    public RefundRagProperties getRag() {
        return rag;
    }

    public void setRag(RefundRagProperties rag) {
        this.rag = rag;
    }

    public RefundModelRegistryProperties getModel() {
        return model;
    }

    public void setModel(RefundModelRegistryProperties model) {
        this.model = model;
    }

    public static class RefundIntentProperties {
        private String algorithm = "NAIVE_BAYES";
        private double threshold = 0.6;

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }
    }

    public static class RefundRagProperties {
        private Map<String, List<String>> collections = new HashMap<>();

        public Map<String, List<String>> getCollections() {
            return collections;
        }

        public void setCollections(Map<String, List<String>> collections) {
            this.collections = collections;
        }
    }

    public static class RefundModelRegistryProperties {
        private Map<String, RefundAgentProfileProperties> profiles = new HashMap<>();

        public Map<String, RefundAgentProfileProperties> getProfiles() {
            return profiles;
        }

        public void setProfiles(Map<String, RefundAgentProfileProperties> profiles) {
            this.profiles = profiles;
        }
    }

    public static class RefundAgentProfileProperties {
        private String base;
        private String ft;
        private String rag;
        private int maxTokens = 256;
        private double temperature = 0.1;

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }

        public String getFt() {
            return ft;
        }

        public void setFt(String ft) {
            this.ft = ft;
        }

        public String getRag() {
            return rag;
        }

        public void setRag(String rag) {
            this.rag = rag;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
}
