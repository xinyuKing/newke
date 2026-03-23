package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.RefundIntentModelState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundIntentModelStateRepository extends JpaRepository<RefundIntentModelState, Long> {
    Optional<RefundIntentModelState> findTopByModelVersionOrderByUpdatedAtDesc(String modelVersion);
}
