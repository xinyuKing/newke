package com.shixi.ecommerce.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.fine-tune")
public class FineTuneProperties {
    private String executable;
    private List<String> args = new ArrayList<>();
    private long timeoutMinutes = 120;
    private String allowedDatasetRoot;

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args == null ? new ArrayList<>() : new ArrayList<>(args);
    }

    public long getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(long timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public String getAllowedDatasetRoot() {
        return allowedDatasetRoot;
    }

    public void setAllowedDatasetRoot(String allowedDatasetRoot) {
        this.allowedDatasetRoot = allowedDatasetRoot;
    }

    public boolean isConfigured() {
        return executable != null && !executable.isBlank();
    }
}
