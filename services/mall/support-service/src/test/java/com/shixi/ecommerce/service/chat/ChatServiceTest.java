package com.shixi.ecommerce.service.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.ChatMessage;
import com.shixi.ecommerce.domain.ChatSession;
import com.shixi.ecommerce.domain.ChatSessionStatus;
import com.shixi.ecommerce.repository.ChatMessageRepository;
import com.shixi.ecommerce.repository.ChatSessionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(sessionRepository, messageRepository);
    }

    @Test
    void listMessagesForUserRejectsForeignSession() {
        when(sessionRepository.findBySessionId("SESSION-1")).thenReturn(Optional.of(session("SESSION-1", 99L)));

        assertThrows(BusinessException.class, () -> chatService.listMessagesForUser(42L, "SESSION-1"));
        verify(messageRepository, never()).findBySessionIdOrderByCreatedAtAsc("SESSION-1");
    }

    @Test
    void listMessagesForUserReturnsMessagesWhenOwned() {
        ChatMessage message = new ChatMessage();
        message.setSessionId("SESSION-1");
        message.setContent("hello");
        when(sessionRepository.findBySessionId("SESSION-1")).thenReturn(Optional.of(session("SESSION-1", 42L)));
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc("SESSION-1")).thenReturn(List.of(message));

        List<ChatMessage> actual = chatService.listMessagesForUser(42L, "SESSION-1");

        assertEquals(1, actual.size());
        assertEquals("hello", actual.get(0).getContent());
    }

    @Test
    void supportSendRejectsSessionAssignedToAnotherSupport() {
        ChatSession session = session("SESSION-1", 42L);
        session.setSupportId(9L);
        when(sessionRepository.findBySessionId("SESSION-1")).thenReturn(Optional.of(session));

        assertThrows(BusinessException.class, () -> chatService.supportSend(7L, "SESSION-1", "reply"));
        verify(messageRepository, never()).save(org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    @Test
    void listMessagesForSupportRejectsForeignAssignedSession() {
        ChatSession session = session("SESSION-1", 42L);
        session.setSupportId(9L);
        when(sessionRepository.findBySessionId("SESSION-1")).thenReturn(Optional.of(session));

        assertThrows(BusinessException.class, () -> chatService.listMessagesForSupport(7L, "SESSION-1"));
        verify(messageRepository, never()).findBySessionIdOrderByCreatedAtAsc("SESSION-1");
    }

    private ChatSession session(String sessionId, Long userId) {
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setStatus(ChatSessionStatus.OPEN);
        return session;
    }
}
