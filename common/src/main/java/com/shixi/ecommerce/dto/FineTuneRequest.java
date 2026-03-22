package com.shixi.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;

public class FineTuneRequest {
    @NotBlank
    private String datasetPath;

    @NotBlank
    private String baseModel;

    public String getDatasetPath() {
        return datasetPath;
    }

    public void setDatasetPath(String datasetPath) {
        this.datasetPath = datasetPath;
    }

    public String getBaseModel() {
        return baseModel;
    }

    public void setBaseModel(String baseModel) {
        this.baseModel = baseModel;
    }
}
