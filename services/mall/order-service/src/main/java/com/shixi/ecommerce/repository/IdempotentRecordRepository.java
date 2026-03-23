package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.IdempotentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotentRecordRepository extends JpaRepository<IdempotentRecord, Long> {
    Optional<IdempotentRecord> findByBizKey(String bizKey);
}
