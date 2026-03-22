package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.config.RefundModelProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagService {
    private final Map<String, List<String>> corpora;

    public RagService(RefundModelProperties properties) {
        this.corpora = properties.getRag().getCollections();
    }

    public List<String> retrieve(String query, String collection) {
        List<String> docs = corpora.getOrDefault(collection, List.of());
        if (query == null || query.isBlank()) {
            return docs.stream().limit(2).collect(Collectors.toList());
        }
        String lower = query.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String doc : docs) {
            if (doc.toLowerCase(Locale.ROOT).contains(lower)) {
                matches.add(doc);
            }
        }
        if (matches.isEmpty()) {
            return docs.stream().limit(2).collect(Collectors.toList());
        }
        return matches.stream().limit(3).collect(Collectors.toList());
    }
}
