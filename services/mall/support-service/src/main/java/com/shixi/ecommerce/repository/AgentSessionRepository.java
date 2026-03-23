package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.AgentSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {
    Optional<AgentSession> findBySessionId(String sessionId);
}
