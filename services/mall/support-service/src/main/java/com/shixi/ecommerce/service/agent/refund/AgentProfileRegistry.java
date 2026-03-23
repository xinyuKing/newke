package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.config.RefundModelProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AgentProfileRegistry {
    private final Map<String, AgentProfile> profiles = new HashMap<>();

    public AgentProfileRegistry(RefundModelProperties properties) {
        Map<String, RefundModelProperties.RefundAgentProfileProperties> configured = properties.getModel().getProfiles();
        for (Map.Entry<String, RefundModelProperties.RefundAgentProfileProperties> entry : configured.entrySet()) {
            RefundModelProperties.RefundAgentProfileProperties value = entry.getValue();
            profiles.put(entry.getKey(),
                    new AgentProfile(entry.getKey(),
                            value.getBase(),
                            value.getFt(),
                            value.getRag(),
                            value.getMaxTokens(),
                            value.getTemperature()));
        }
    }

    public AgentProfile getProfile(String agentType) {
        return profiles.get(agentType);
    }
}
