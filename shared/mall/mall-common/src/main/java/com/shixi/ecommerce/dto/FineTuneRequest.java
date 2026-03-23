package com.shixi.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FineTuneRequest {
    @NotBlank
    @Size(max = 512)
    private String datasetPath;

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "[A-Za-z0-9._:-]+")
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
