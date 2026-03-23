package com.shixi.ecommerce.service.agent;

import com.shixi.ecommerce.domain.AgentSession;
import com.shixi.ecommerce.domain.IntentType;
import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.repository.AgentSessionRepository;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AgentSessionService {
    private static final String SESSION_STATE_PREFIX = "agent:session:state:";
    private final AgentSessionRepository repository;
    private final StringRedisTemplate redisTemplate;

    public AgentSessionService(AgentSessionRepository repository, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    public AgentSession getOrCreate(String sessionId, IntentType intent) {
        String resolvedId = (sessionId == null || sessionId.isBlank()) ? generateSessionId() : sessionId;
        AgentSession session = repository.findBySessionId(resolvedId).orElseGet(AgentSession::new);
        session.setSessionId(resolvedId);
        if (session.getState() == null) {
            session.setState(SessionState.INIT);
        }
        session.setLastIntent(intent);
        repository.save(session);
        cacheState(resolvedId, session.getState());
        return session;
    }

    public void updateState(String sessionId, SessionState state, IntentType intent) {
        AgentSession session = repository.findBySessionId(sessionId).orElseGet(AgentSession::new);
        session.setSessionId(sessionId);
        session.setState(state);
        session.setLastIntent(intent);
        repository.save(session);
        cacheState(sessionId, state);
    }

    private void cacheState(String sessionId, SessionState state) {
        redisTemplate.opsForValue().set(SESSION_STATE_PREFIX + sessionId, state.name(), Duration.ofHours(12));
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
