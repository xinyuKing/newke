package com.shixi.ecommerce.dto;

import com.shixi.ecommerce.domain.FineTuneStatus;

public class FineTuneResponse {
    private String jobId;
    private FineTuneStatus status;

    public FineTuneResponse(String jobId, FineTuneStatus status) {
        this.jobId = jobId;
        this.status = status;
    }

    public String getJobId() {
        return jobId;
    }

    public FineTuneStatus getStatus() {
        return status;
    }
}
