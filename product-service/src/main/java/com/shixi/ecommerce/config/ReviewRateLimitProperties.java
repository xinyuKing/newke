package com.shixi.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 评价接口限流配置。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Component
@ConfigurationProperties(prefix = "review.rate-limit")
public class ReviewRateLimitProperties {
    private int windowSeconds = 60;
    private int userMax = 20;
    private int ipMax = 40;
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
