package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {
    Optional<AgentSession> findBySessionId(String sessionId);
}
