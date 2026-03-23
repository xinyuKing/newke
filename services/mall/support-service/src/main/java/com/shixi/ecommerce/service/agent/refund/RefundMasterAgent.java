package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.SessionState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RefundMasterAgent {
    private final RefundMessageParser parser;
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;

    public RefundMasterAgent(RefundMessageParser parser,
                             AgentProfileRegistry profileRegistry,
                             RagService ragService,
                             ModelClient modelClient) {
        this.parser = parser;
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
    }

    public RefundTaskPlan plan(RefundContext context) {
        parser.enrich(context);
        ensureDefaultRequestType(context);

        List<RefundTaskType> tasks = new ArrayList<>();
        SessionState state;
        MasterSummary summary;

        if (!context.hasSlot(RefundSlots.ORDER_NO)) {
            tasks.add(RefundTaskType.ASK_ORDER);
            state = SessionState.WAIT_ORDER;
            summary = buildSummary("Need order number first.", context);
        } else if (!context.hasSlot(RefundSlots.REASON)) {
            tasks.add(RefundTaskType.ASK_REASON);
            state = SessionState.WAIT_REASON;
            summary = buildSummary("Order received, need refund reason.", context);
        } else if (needsEvidence(context) && !context.hasSlot(RefundSlots.EVIDENCE)) {
            tasks.add(RefundTaskType.ASK_EVIDENCE);
            state = SessionState.WAIT_EVIDENCE;
            summary = buildSummary("Evidence required for quality issue.", context);
        } else {
            tasks.add(RefundTaskType.POLICY_CHECK);
            tasks.add(RefundTaskType.LOGISTICS_CHECK);
            tasks.add(RefundTaskType.RISK_CHECK);
            state = SessionState.REVIEWING;
            summary = buildSummary("Gather policy, logistics, and risk signals.", context);
        }

        context.setState(state);
        return new RefundTaskPlan(tasks, state, summary.text(), summary.meta());
    }

    private void ensureDefaultRequestType(RefundContext context) {
        if (context.hasSlot(RefundSlots.REQUEST_TYPE)) {
            return;
        }
        String reason = context.getSlot(RefundSlots.REASON);
        if (RefundReasonType.NOT_RECEIVED.name().equals(reason)) {
            context.putSlot(RefundSlots.REQUEST_TYPE, RefundRequestType.REFUND_ONLY.name());
        } else if (reason != null) {
            context.putSlot(RefundSlots.REQUEST_TYPE, RefundRequestType.RETURN_REFUND.name());
        }
    }

    private boolean needsEvidence(RefundContext context) {
        String reason = context.getSlot(RefundSlots.REASON);
        return RefundReasonType.QUALITY.name().equals(reason)
                || RefundReasonType.WRONG_ITEM.name().equals(reason)
                || RefundReasonType.MISSING_PARTS.name().equals(reason);
    }

    private MasterSummary buildSummary(String prompt, RefundContext context) {
        AgentProfile profile = profileRegistry.getProfile(RefundAgentTypes.MASTER);
        List<String> docs = ragService.retrieve(prompt, profile.getRagCollection());
        String base = modelClient.generate(profile, prompt, docs);
        String meta = String.join(" | ", docs);
        if (context.getFeedbackHints().isEmpty()) {
            return new MasterSummary(base, meta);
        }
        return new MasterSummary(base + " feedback=" + String.join("; ", context.getFeedbackHints()), meta);
    }

    private record MasterSummary(String text, String meta) {
    }
}
