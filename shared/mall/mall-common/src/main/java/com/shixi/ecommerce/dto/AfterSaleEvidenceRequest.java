package com.shixi.ecommerce.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public class AfterSaleEvidenceRequest {
    @Size(max = 512)
    private String evidenceNote;

    @Size(max = 5)
    private List<@Size(max = 512) String> evidenceUrls;

    public String getEvidenceNote() {
        return evidenceNote;
    }

    public void setEvidenceNote(String evidenceNote) {
        this.evidenceNote = evidenceNote;
    }

    public List<String> getEvidenceUrls() {
        return evidenceUrls;
    }

    public void setEvidenceUrls(List<String> evidenceUrls) {
        this.evidenceUrls = evidenceUrls;
    }
}
