package com.shixi.ecommerce.web;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.dto.AgentChatRequest;
import com.shixi.ecommerce.dto.AgentChatResponse;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.agent.AgentOrchestrator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentOrchestrator orchestrator;
    private final CurrentUserService currentUserService;

    public AgentController(AgentOrchestrator orchestrator, CurrentUserService currentUserService) {
        this.orchestrator = orchestrator;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        var user = currentUserService.getCurrentUser();
        return ApiResponse.ok(orchestrator.chat(user.getUserId(), user.getRole(), request));
    }
}
