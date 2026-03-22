package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.Role;
import com.shixi.ecommerce.domain.UserAccount;
import com.shixi.ecommerce.dto.LoginRequest;
import com.shixi.ecommerce.dto.LoginResponse;
import com.shixi.ecommerce.dto.RegisterRequest;
import com.shixi.ecommerce.integration.CommunityAuthUser;
import com.shixi.ecommerce.integration.CommunityUserClient;
import com.shixi.ecommerce.repository.UserAccountRepository;
import com.shixi.ecommerce.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Coordinates authentication between the forum user center and the mall auth module.
 *
 * <p>The forum user service is the source of truth for usernames, passwords, activation state
 * and base profile information. This service only keeps a local projection of the same account
 * so downstream mall services can issue JWTs and perform role-based authorization locally.</p>
 */
@Service
public class AuthService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CommunityUserClient communityUserClient;

    public AuthService(UserAccountRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       CommunityUserClient communityUserClient) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.communityUserClient = communityUserClient;
    }

    /**
     * Registers a mall user by delegating the actual account creation to the forum.
     *
     * <p>Self-registration is limited to normal users. After the forum creates the account, the
     * mall immediately creates or refreshes the local account snapshot with the same user ID to
     * keep both systems aligned.</p>
     *
     * @param request registration request submitted by the client
     */
    public void register(RegisterRequest request) {
        if (request.getRole() != Role.USER) {
            throw new BusinessException("Only USER registration is allowed");
        }
        CommunityAuthUser communityUser = communityUserClient.register(request.getUsername(), request.getPassword(), null);
        upsertLocalAccount(communityUser, request.getPassword());
    }

    /**
     * Authenticates against the forum user service and issues a mall JWT after local sync.
     *
     * <p>The mall never validates the credential against its own database first. Instead, it asks
     * the forum to verify the password, then upserts the mirrored local account and generates the
     * token used by mall-side services.</p>
     *
     * @param request login request submitted by the client
     * @return JWT and the resolved mall role
     */
    public LoginResponse login(LoginRequest request) {
        CommunityAuthUser communityUser = communityUserClient.login(request.getUsername(), request.getPassword());
        UserAccount account = upsertLocalAccount(communityUser, request.getPassword());
        Role resolvedRole = resolveRole(account, communityUser.getType());
        account.setRole(resolvedRole);
        repository.save(account);
        String token = jwtTokenProvider.createToken(account.getUsername(), account.getId(), resolvedRole);
        return new LoginResponse(token, resolvedRole.name());
    }

    /**
     * Creates or refreshes the mall-side account projection from forum user data.
     *
     * <p>The local account uses the forum user ID as its primary key so orders, carts and other
     * mall data can reference the same identity. When a record does not exist locally, it will be
     * created lazily during registration or first login.</p>
     *
     * @param communityUser user data returned by the forum user service
     * @param rawPassword clear text password from the current request, used only when the local
     *                    password hash has not been initialized yet
     * @return persisted local account snapshot
     */
    private UserAccount upsertLocalAccount(CommunityAuthUser communityUser, String rawPassword) {
        if (communityUser == null) {
            throw new BusinessException("Community user not found");
        }
        Long communityId = communityUser.getId() == null ? null : communityUser.getId().longValue();
        if (communityId == null) {
            throw new BusinessException("Community user id missing");
        }
        UserAccount account = repository.findById(communityId).orElse(null);
        if (account == null) {
            // Reject a conflicting username mapping to prevent one mall account from pointing to
            // a different forum user ID.
            UserAccount byUsername = repository.findByUsername(communityUser.getUsername()).orElse(null);
            if (byUsername != null && !Objects.equals(byUsername.getId(), communityId)) {
                throw new BusinessException("Username already mapped to another user");
            }
            account = new UserAccount();
            account.setId(communityId);
        }
        account.setUsername(communityUser.getUsername());
        if (communityUser.getEmail() != null) {
            account.setEmail(communityUser.getEmail());
        }
        if (communityUser.getHeaderUrl() != null) {
            account.setAvatarUrl(communityUser.getHeaderUrl());
        }
        account.setEnabled(communityUser.getStatus() != null && communityUser.getStatus() == 1);
        if (account.getPasswordHash() == null || account.getPasswordHash().isBlank()) {
            // The forum remains the credential authority. The local hash only exists so mall-side
            // security components can work without designing a second identity source.
            String raw = rawPassword == null ? UUID.randomUUID().toString() : rawPassword;
            account.setPasswordHash(passwordEncoder.encode(raw));
        }
        if (account.getRole() == null) {
            account.setRole(resolveRole(account, communityUser.getType()));
        }
        return repository.save(account);
    }

    /**
     * Resolves the effective mall role for the current account snapshot.
     *
     * <p>If the local account has already been elevated to a privileged role, that local decision
     * is preserved. Otherwise, the value falls back to the role translated from the forum user
     * type.</p>
     *
     * @param account local mall account projection
     * @param communityType role type returned by the forum
     * @return resolved mall role
     */
    private Role resolveRole(UserAccount account, Integer communityType) {
        if (account != null && account.getRole() != null && account.getRole() != Role.USER) {
            return account.getRole();
        }
        return mapRole(communityType);
    }

    /**
     * Maps forum user types to mall roles.
     *
     * @param communityType forum-side user type
     * @return mall role used by JWT and authorization checks
     */
    private Role mapRole(Integer communityType) {
        if (communityType == null) {
            return Role.USER;
        }
        switch (communityType) {
            case 1:
                return Role.ADMIN;
            case 2:
                return Role.SUPPORT;
            default:
                return Role.USER;
        }
    }
}
