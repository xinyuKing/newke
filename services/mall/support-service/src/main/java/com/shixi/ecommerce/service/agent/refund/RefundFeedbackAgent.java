package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.SessionState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class RefundFeedbackAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;
    private final RefundContextValidator contextValidator;

    public RefundFeedbackAgent(AgentProfileRegistry profileRegistry,
                               RagService ragService,
                               ModelClient modelClient,
                               RefundContextValidator contextValidator) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
        this.contextValidator = contextValidator;
    }

    public FeedbackResult evaluate(RefundContext context, RefundTaskPlan plan, String reply) {
        if (reply == null || reply.isBlank()) {
            return new FeedbackResult(false, "Empty reply");
        }
        String text = reply.toLowerCase(Locale.ROOT);
        SessionState state = plan.getNextState();

        if (contextValidator != null) {
            List<String> errors = contextValidator.validate(context, state);
            errors.addAll(contextValidator.validateDecision(context, context.getDecision()));
            if (!errors.isEmpty()) {
                return new FeedbackResult(false, String.join("; ", errors));
            }
        }

        if (state == SessionState.WAIT_ORDER && !containsAny(text, "order", "\u8ba2\u5355", "\u5355\u53f7")) {
            return new FeedbackResult(false, "Must ask for order number");
        }
        if (state == SessionState.WAIT_REASON && !containsAny(text, "reason", "\u539f\u56e0")) {
            return new FeedbackResult(false, "Must ask for refund reason");
        }
        if (state == SessionState.WAIT_EVIDENCE
                && !containsAny(text, "photo", "video", "evidence", "\u7167\u7247", "\u51ed\u8bc1", "\u56fe\u7247")) {
            return new FeedbackResult(false, "Must request evidence");
        }

        AgentProfile profile = profileRegistry.getProfile(RefundAgentTypes.FEEDBACK);
        String prompt = "Feedback ok for state=" + state;
        List<String> docs = ragService.retrieve(prompt, profile.getRagCollection());
        modelClient.generate(profile, prompt, docs);
        return new FeedbackResult(true, "OK", String.join(" | ", docs));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
