package com.shixi.ecommerce.service;

import com.shixi.ecommerce.common.BusinessException;
import com.shixi.ecommerce.domain.Role;
import com.shixi.ecommerce.domain.UserAccount;
import com.shixi.ecommerce.dto.AdminUserResponse;
import com.shixi.ecommerce.repository.UserAccountRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {
    private final UserAccountRepository repository;

    public AdminUserService(UserAccountRepository repository) {
        this.repository = repository;
    }

    public List<AdminUserResponse> listUsers() {
        return repository.findAll().stream()
                .map(user -> new AdminUserResponse(user.getId(), user.getUsername(), user.getRole(), user.isEnabled()))
                .collect(Collectors.toList());
    }

    public void updateRole(Long userId, Role role) {
        UserAccount account = repository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        account.setRole(role);
        repository.save(account);
    }

    public void updateStatus(Long userId, boolean enabled) {
        UserAccount account = repository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        account.setEnabled(enabled);
        repository.save(account);
    }
}
