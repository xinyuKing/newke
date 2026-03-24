package com.shixi.ecommerce.service.agent;

import com.shixi.ecommerce.common.BusinessException;
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

    public AgentSession getOrCreate(String sessionId, Long userId, IntentType intent) {
        requireUserId(userId);
        String resolvedId = (sessionId == null || sessionId.isBlank()) ? generateSessionId() : sessionId;
        AgentSession session = repository.findBySessionId(resolvedId).orElseGet(() -> newSession(resolvedId, userId));
        ensureOwned(session, userId);
        if (session.getState() == null) {
            session.setState(SessionState.INIT);
        }
        session.setLastIntent(intent);
        repository.save(session);
        cacheState(resolvedId, session.getState());
        return session;
    }

    public void updateState(String sessionId, Long userId, SessionState state, IntentType intent) {
        requireUserId(userId);
        AgentSession session = repository.findBySessionId(sessionId).orElseGet(() -> newSession(sessionId, userId));
        ensureOwned(session, userId);
        session.setState(state);
        session.setLastIntent(intent);
        repository.save(session);
        cacheState(sessionId, state);
    }

    private AgentSession newSession(String sessionId, Long userId) {
        AgentSession session = new AgentSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        return session;
    }

    private void ensureOwned(AgentSession session, Long userId) {
        if (session.getUserId() == null) {
            throw new BusinessException("Session owner missing");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("Session not owned by user");
        }
    }

    private void cacheState(String sessionId, SessionState state) {
        redisTemplate.opsForValue().set(SESSION_STATE_PREFIX + sessionId, state.name(), Duration.ofHours(12));
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("UserId required");
        }
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
