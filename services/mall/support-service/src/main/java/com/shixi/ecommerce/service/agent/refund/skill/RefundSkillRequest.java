package com.shixi.ecommerce.service.agent.refund.skill;

import com.shixi.ecommerce.service.agent.refund.RefundAgentOutput;
import com.shixi.ecommerce.service.agent.refund.RefundContext;
import com.shixi.ecommerce.service.agent.refund.RefundTaskPlan;
import java.util.List;

public final class RefundSkillRequest {
    private final RefundContext context;
    private final RefundTaskPlan plan;
    private final List<RefundAgentOutput> outputs;
    private final String reply;

    private RefundSkillRequest(Builder builder) {
        this.context = builder.context;
        this.plan = builder.plan;
        this.outputs = builder.outputs == null ? List.of() : List.copyOf(builder.outputs);
        this.reply = builder.reply;
    }

    public static Builder builder(RefundContext context) {
        return new Builder(context);
    }

    public RefundContext getContext() {
        return context;
    }

    public RefundTaskPlan getPlan() {
        return plan;
    }

    public List<RefundAgentOutput> getOutputs() {
        return outputs;
    }

    public String getReply() {
        return reply;
    }

    public static final class Builder {
        private final RefundContext context;
        private RefundTaskPlan plan;
        private List<RefundAgentOutput> outputs;
        private String reply;

        private Builder(RefundContext context) {
            this.context = context;
        }

        public Builder plan(RefundTaskPlan plan) {
            this.plan = plan;
            return this;
        }

        public Builder outputs(List<RefundAgentOutput> outputs) {
            this.outputs = outputs;
            return this;
        }

        public Builder reply(String reply) {
            this.reply = reply;
            return this;
        }

        public RefundSkillRequest build() {
            return new RefundSkillRequest(this);
        }
    }
}
