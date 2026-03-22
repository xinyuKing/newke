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

    public void register(RegisterRequest request) {
        if (request.getRole() != Role.USER) {
            throw new BusinessException("Only USER registration is allowed");
        }
        CommunityAuthUser communityUser = communityUserClient.register(request.getUsername(), request.getPassword(), null);
        upsertLocalAccount(communityUser, request.getPassword());
    }

    public LoginResponse login(LoginRequest request) {
        CommunityAuthUser communityUser = communityUserClient.login(request.getUsername(), request.getPassword());
        UserAccount account = upsertLocalAccount(communityUser, request.getPassword());
        Role resolvedRole = resolveRole(account, communityUser.getType());
        account.setRole(resolvedRole);
        repository.save(account);
        String token = jwtTokenProvider.createToken(account.getUsername(), account.getId(), resolvedRole);
        return new LoginResponse(token, resolvedRole.name());
    }

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
            String raw = rawPassword == null ? UUID.randomUUID().toString() : rawPassword;
            account.setPasswordHash(passwordEncoder.encode(raw));
        }
        if (account.getRole() == null) {
            account.setRole(resolveRole(account, communityUser.getType()));
        }
        return repository.save(account);
    }

    private Role resolveRole(UserAccount account, Integer communityType) {
        if (account != null && account.getRole() != null && account.getRole() != Role.USER) {
            return account.getRole();
        }
        return mapRole(communityType);
    }

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
