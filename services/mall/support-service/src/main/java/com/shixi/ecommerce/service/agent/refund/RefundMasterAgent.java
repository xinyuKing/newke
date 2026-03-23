package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillNames;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillRegistry;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RefundMasterAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;
    private final RefundSkillRegistry skillRegistry;

    public RefundMasterAgent(AgentProfileRegistry profileRegistry,
                             RagService ragService,
                             ModelClient modelClient,
                             RefundSkillRegistry skillRegistry) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
        this.skillRegistry = skillRegistry;
    }

    public RefundTaskPlan plan(RefundContext context) {
        skillRegistry.execute(
                RefundSkillNames.EXTRACT_REFUND_SLOTS,
                RefundSkillRequest.builder(context).build(),
                RefundContext.class);

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
