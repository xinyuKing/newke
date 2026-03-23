package com.shixi.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "order.rate-limit")
public class OrderRateLimitProperties {
    private int windowSeconds = 60;
    private int userMax = 30;
    private int ipMax = 60;
    private String algorithm = "fixed";

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getUserMax() {
        return userMax;
    }

    public void setUserMax(int userMax) {
        this.userMax = userMax;
    }

    public int getIpMax() {
        return ipMax;
    }

    public void setIpMax(int ipMax) {
        this.ipMax = ipMax;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
