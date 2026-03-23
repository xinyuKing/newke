package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillNames;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillOutput;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillRegistry;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RefundFusionAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;
    private final RefundSkillRegistry skillRegistry;

    public RefundFusionAgent(AgentProfileRegistry profileRegistry,
                             RagService ragService,
                             ModelClient modelClient,
                             RefundSkillRegistry skillRegistry) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
        this.skillRegistry = skillRegistry;
    }

    public FusionResult fuse(RefundContext context, RefundTaskPlan plan, List<RefundAgentOutput> outputs) {
        RefundSkillOutput skillOutput = skillRegistry.execute(
                RefundSkillNames.COMPOSE_REPLY,
                RefundSkillRequest.builder(context).plan(plan).outputs(outputs).build(),
                RefundSkillOutput.class);
        AgentProfile profile = profileRegistry.getProfile(RefundAgentTypes.FUSION);
        String prompt = skillOutput.getPrompt();
        List<String> docs = ragService.retrieve(prompt, profile.getRagCollection());
        String result = modelClient.generate(profile, prompt, docs);
        return new FusionResult(result, String.join(" | ", docs));
    }
}
