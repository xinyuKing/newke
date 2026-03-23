package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.SessionState;
import java.util.List;

public class RefundTaskPlan {
    private final List<RefundTaskType> tasks;
    private final SessionState nextState;
    private final String summary;
    private final String masterMeta;

    public RefundTaskPlan(List<RefundTaskType> tasks, SessionState nextState, String summary, String masterMeta) {
        this.tasks = tasks;
        this.nextState = nextState;
        this.summary = summary;
        this.masterMeta = masterMeta;
    }

    public List<RefundTaskType> getTasks() {
        return tasks;
    }

    public SessionState getNextState() {
        return nextState;
    }

    public String getSummary() {
        return summary;
    }

    public String getMasterMeta() {
        return masterMeta;
    }
}
