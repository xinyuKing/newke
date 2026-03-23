package com.shixi.ecommerce.service.agent.refund;

import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillNames;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillOutput;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillRegistry;
import com.shixi.ecommerce.service.agent.refund.skill.RefundSkillRequest;
import org.springframework.stereotype.Service;

@Service
public class RefundPolicyAgent implements RefundSubAgent {
    private final AgentProfileRegistry profileRegistry;
    private final RagService ragService;
    private final ModelClient modelClient;
    private final RefundSkillRegistry skillRegistry;

    public RefundPolicyAgent(AgentProfileRegistry profileRegistry,
                             RagService ragService,
                             ModelClient modelClient,
                             RefundSkillRegistry skillRegistry) {
        this.profileRegistry = profileRegistry;
        this.ragService = ragService;
        this.modelClient = modelClient;
        this.skillRegistry = skillRegistry;
    }

    @Override
    public String getType() {
        return RefundAgentTypes.POLICY;
    }

    @Override
    public RefundAgentOutput handle(RefundContext context) {
        RefundSkillOutput skillOutput = skillRegistry.execute(
                RefundSkillNames.EVALUATE_POLICY,
                RefundSkillRequest.builder(context).build(),
                RefundSkillOutput.class);
        AgentProfile profile = profileRegistry.getProfile(getType());
        String prompt = skillOutput.getPrompt();
        var docs = ragService.retrieve(prompt, profile.getRagCollection());
        String text = modelClient.generate(profile, prompt, docs);
        return new RefundAgentOutput(
                text,
                skillOutput.getUpdates(),
                skillOutput.getDecision(),
                String.join(" | ", docs));
    }
}
