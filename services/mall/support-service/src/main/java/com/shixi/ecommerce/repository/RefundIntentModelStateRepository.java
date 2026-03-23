package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.RefundIntentModelState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundIntentModelStateRepository extends JpaRepository<RefundIntentModelState, Long> {
    Optional<RefundIntentModelState> findTopByModelVersionOrderByUpdatedAtDesc(String modelVersion);
}
