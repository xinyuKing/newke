package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.CheckoutRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutRecordRepository extends JpaRepository<CheckoutRecord, Long> {
    Optional<CheckoutRecord> findByBizKey(String bizKey);
}
