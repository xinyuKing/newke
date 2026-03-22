package com.shixi.ecommerce.service.agent.refund;

public class AgentProfile {
    private final String agentType;
    private final String baseModel;
    private final String fineTuneModel;
    private final String ragCollection;
    private final int maxTokens;
    private final double temperature;

    public AgentProfile(String agentType,
                        String baseModel,
                        String fineTuneModel,
                        String ragCollection,
                        int maxTokens,
                        double temperature) {
        this.agentType = agentType;
        this.baseModel = baseModel;
        this.fineTuneModel = fineTuneModel;
        this.ragCollection = ragCollection;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
    }

    public String getAgentType() {
        return agentType;
    }

    public String getBaseModel() {
        return baseModel;
    }

    public String getFineTuneModel() {
        return fineTuneModel;
    }

    public String getRagCollection() {
        return ragCollection;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }
}
