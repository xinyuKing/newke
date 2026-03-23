package com.shixi.ecommerce.web.user;

import com.shixi.ecommerce.common.ApiResponse;
import com.shixi.ecommerce.domain.ChatMessage;
import com.shixi.ecommerce.domain.ChatSession;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/support")
public class UserChatController {
    private final ChatService chatService;
    private final CurrentUserService currentUserService;

    public UserChatController(ChatService chatService, CurrentUserService currentUserService) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/session")
    public ApiResponse<ChatSessionResponse> createSession() {
        Long userId = currentUserService.getCurrentUser().getUserId();
        ChatSession session = chatService.createSession(userId);
        return ApiResponse.ok(toSessionResponse(session));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionResponse>> listSessions() {
        Long userId = currentUserService.getCurrentUser().getUserId();
        List<ChatSessionResponse> sessions = chatService.listSessionsByUser(userId).stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok(sessions);
    }

    @PostMapping("/message")
    public ApiResponse<ChatMessageResponse> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        ChatMessage message = chatService.userSend(userId, request.getSessionId(), request.getContent());
        return ApiResponse.ok(toMessageResponse(message));
    }

    @GetMapping("/session/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> messages(@PathVariable String sessionId) {
        Long userId = currentUserService.getCurrentUser().getUserId();
        List<ChatMessageResponse> messages = chatService.listMessagesForUser(userId, sessionId).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok(messages);
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
