package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.ChatSession;
import com.shixi.ecommerce.domain.ChatSessionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findBySessionId(String sessionId);

    List<ChatSession> findByStatus(ChatSessionStatus status);

    List<ChatSession> findByUserId(Long userId);
}
