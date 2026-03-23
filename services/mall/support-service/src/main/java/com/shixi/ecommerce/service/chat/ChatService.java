package com.shixi.ecommerce.service.chat;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.ChatMessage;
import com.shixi.ecommerce.domain.ChatSenderRole;
import com.shixi.ecommerce.domain.ChatSession;
import com.shixi.ecommerce.domain.ChatSessionStatus;
import com.shixi.ecommerce.repository.ChatMessageRepository;
import com.shixi.ecommerce.repository.ChatSessionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public ChatService(ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public ChatSession createSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setStatus(ChatSessionStatus.OPEN);
        return sessionRepository.save(session);
    }

    @Transactional
    public ChatMessage userSend(Long userId, String sessionId, String content) {
        ChatSession session = getSessionOrThrow(sessionId);
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("Session not owned by user");
        }
        if (session.getStatus() == ChatSessionStatus.CLOSED) {
            throw new BusinessException("Session closed");
        }
        return saveMessage(sessionId, ChatSenderRole.USER, userId, content);
    }

    @Transactional
    public ChatMessage supportSend(Long supportId, String sessionId, String content) {
        ChatSession session = getSessionOrThrow(sessionId);
        if (session.getStatus() == ChatSessionStatus.CLOSED) {
            throw new BusinessException("Session closed");
        }
        if (session.getSupportId() == null) {
            session.setSupportId(supportId);
            sessionRepository.save(session);
        }
        return saveMessage(sessionId, ChatSenderRole.SUPPORT, supportId, content);
    }

    public List<ChatSession> listSessions(ChatSessionStatus status) {
        if (status == null) {
            return sessionRepository.findAll();
        }
        return sessionRepository.findByStatus(status);
    }

    public List<ChatSession> listSessionsByUser(Long userId) {
        return sessionRepository.findByUserId(userId);
    }

    public List<ChatMessage> listMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public List<ChatMessage> listMessagesForUser(Long userId, String sessionId) {
        ChatSession session = getSessionOrThrow(sessionId);
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("Session not owned by user");
        }
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public void closeSession(String sessionId) {
        ChatSession session = getSessionOrThrow(sessionId);
        session.setStatus(ChatSessionStatus.CLOSED);
        sessionRepository.save(session);
    }

    private ChatMessage saveMessage(String sessionId, ChatSenderRole role, Long senderId, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setSenderRole(role);
        message.setSenderId(senderId);
        message.setContent(content);
        return messageRepository.save(message);
    }

    private ChatSession getSessionOrThrow(String sessionId) {
        return sessionRepository
                .findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found"));
    }
}
