package com.shixi.ecommerce.web.support;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.domain.ChatMessage;
import com.shixi.ecommerce.domain.ChatSession;
import com.shixi.ecommerce.domain.ChatSessionStatus;
import com.shixi.ecommerce.dto.ChatMessageRequest;
import com.shixi.ecommerce.dto.ChatMessageResponse;
import com.shixi.ecommerce.dto.ChatSessionResponse;
import com.shixi.ecommerce.service.CurrentUserService;
import com.shixi.ecommerce.service.chat.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/chat")
public class SupportChatController {
    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public SupportChatController(ChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionResponse>> listSessions(
            @RequestParam(required = false) ChatSessionStatus status) {
        List<ChatSessionResponse> sessions = chatService.listSessions(status).stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok(sessions);
    }

    @PostMapping("/message")
    public ApiResponse<ChatMessageResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        Long supportId = currentUserService.getCurrentUser().getUserId();
        ChatMessage message = chatService.supportSend(supportId, request.getSessionId(), request.getContent());
        return ApiResponse.ok(toMessageResponse(message));
    }

    @PostMapping("/session/{sessionId}/claim")
    public ApiResponse<ChatSessionResponse> claim(@PathVariable String sessionId) {
        Long supportId = currentUserService.getCurrentUser().getUserId();
        ChatSession session = chatService.claimSession(supportId, sessionId);
        return ApiResponse.ok(toSessionResponse(session));
    }

    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> messages(@PathVariable String sessionId) {
        Long supportId = currentUserService.getCurrentUser().getUserId();
        List<ChatMessageResponse> messages = chatService.listMessagesForSupport(supportId, sessionId).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok(messages);
    }

    @PostMapping("/session/{sessionId}/close")
    public ApiResponse<String> close(@PathVariable String sessionId) {
        Long supportId = currentUserService.getCurrentUser().getUserId();
        chatService.closeSession(supportId, sessionId);
        return ApiResponse.ok("OK");
    }

    private ChatSessionResponse toSessionResponse(ChatSession session) {
        return new ChatSessionResponse(
                session.getSessionId(),
                session.getUserId(),
                session.getSupportId(),
                session.getStatus(),
                session.getUpdatedAt());
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSessionId(),
                message.getSenderRole(),
                message.getSenderId(),
                message.getContent(),
                message.getCreatedAt());
    }
}
