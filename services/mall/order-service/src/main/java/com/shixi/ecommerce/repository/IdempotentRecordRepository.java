package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.IdempotentRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotentRecordRepository extends JpaRepository<IdempotentRecord, Long> {
    Optional<IdempotentRecord> findByBizKey(String bizKey);
}
