package com.shixi.ecommerce.service.agent.refund;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelClient {
    public String generate(AgentProfile profile, String prompt, List<String> docs) {
        String ragInfo = docs == null || docs.isEmpty()
                ? "rag=none"
                : "rag=" + docs.stream().limit(2).collect(Collectors.joining(" | "));
        String model = profile == null ? "unknown" : profile.getBaseModel();
        String fineTune = profile == null ? "none" : profile.getFineTuneModel();
        return String.format(
                "[model=%s,ft=%s,%s] %s",
                model,
                fineTune,
                ragInfo,
                prompt
        );
    }
}
