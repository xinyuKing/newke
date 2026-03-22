package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.FineTuneJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FineTuneJobRepository extends JpaRepository<FineTuneJob, Long> {
    Optional<FineTuneJob> findByJobId(String jobId);
}
