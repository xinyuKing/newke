package com.shixi.ecommerce.internal;

import java.util.ArrayList;
import java.util.List;

public class InternalAuthProperties {
    private boolean enabled = true;
    private String serviceId;
    private String secret = "change-this-internal-api-secret-32-bytes";
    private long maxSkewSeconds = 30;
    private List<String> allowedCallers = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getMaxSkewSeconds() {
        return maxSkewSeconds;
    }

    public void setMaxSkewSeconds(long maxSkewSeconds) {
        this.maxSkewSeconds = maxSkewSeconds;
    }

    public List<String> getAllowedCallers() {
        return allowedCallers;
    }

    public void setAllowedCallers(List<String> allowedCallers) {
        this.allowedCallers = allowedCallers == null ? new ArrayList<>() : new ArrayList<>(allowedCallers);
    }
}
