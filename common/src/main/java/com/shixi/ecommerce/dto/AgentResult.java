package com.shixi.ecommerce.dto;

public class AgentResult {
    private String agentType;
    private String output;
    private String meta;

    public AgentResult(String agentType, String output) {
        this.agentType = agentType;
        this.output = output;
    }

    public AgentResult(String agentType, String output, String meta) {
        this.agentType = agentType;
        this.output = output;
        this.meta = meta;
    }

    public String getAgentType() {
        return agentType;
    }

    public String getOutput() {
        return output;
    }

    public String getMeta() {
        return meta;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }
}
