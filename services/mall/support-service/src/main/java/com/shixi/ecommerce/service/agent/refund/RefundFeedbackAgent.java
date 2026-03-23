package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillNames;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillRegistry;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RefundFeedbackAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;
    private final RefundSkillRegistry skillRegistry;

    public RefundFeedbackAgent(AgentProfileRegistry profileRegistry,
                               RagService ragService,
                               ModelClient modelClient,
                               RefundSkillRegistry skillRegistry) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
        this.skillRegistry = skillRegistry;
    }

    public FeedbackResult evaluate(RefundContext context, RefundTaskPlan plan, String reply) {
        FeedbackResult validationResult = skillRegistry.execute(
                RefundSkillNames.VALIDATE_REPLY,
                RefundSkillRequest.builder(context).plan(plan).reply(reply).build(),
                FeedbackResult.class);
        if (!validationResult.isOk()) {
            return validationResult;
        }

        AgentProfile profile = profileRegistry.getProfile(RefundAgentTypes.FEEDBACK);
        SessionState state = plan.getNextState();
        String prompt = "Feedback ok for state=" + state;
        List<String> docs = ragService.retrieve(prompt, profile.getRagCollection());
        modelClient.generate(profile, prompt, docs);
        return new FeedbackResult(true, "OK", String.join(" | ", docs));
    }
}
