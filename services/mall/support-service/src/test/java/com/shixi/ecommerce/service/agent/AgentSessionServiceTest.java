package com.shixi.ecommerce.service.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.AgentSession;
import com.shixi.ecommerce.domain.IntentType;
import com.shixi.ecommerce.domain.SessionState;
import com.shixi.ecommerce.repository.AgentSessionRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AgentSessionServiceTest {

    @Mock
    private AgentSessionRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AgentSessionService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new AgentSessionService(repository, redisTemplate);
    }

    @Test
    void getOrCreateRejectsForeignSession() {
        AgentSession session = new AgentSession();
        session.setSessionId("SESSION-1");
        session.setUserId(99L);
        session.setState(SessionState.INIT);
        when(repository.findBySessionId("SESSION-1")).thenReturn(Optional.of(session));

        assertThrows(BusinessException.class, () -> service.getOrCreate("SESSION-1", 42L, IntentType.REFUND));
        verify(repository, never()).save(any(AgentSession.class));
    }

    @Test
    void getOrCreateStoresOwnerOnNewSession() {
        when(repository.findBySessionId("SESSION-1")).thenReturn(Optional.empty());
        when(repository.save(any(AgentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentSession session = service.getOrCreate("SESSION-1", 42L, IntentType.REFUND);

        assertEquals(42L, session.getUserId());
        assertEquals(SessionState.INIT, session.getState());
        verify(valueOperations).set(eq("agent:session:state:SESSION-1"), eq("INIT"), any(Duration.class));
    }
}
