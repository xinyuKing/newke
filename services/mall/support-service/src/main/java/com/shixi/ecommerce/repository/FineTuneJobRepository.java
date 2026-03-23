package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.FineTuneJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FineTuneJobRepository extends JpaRepository<FineTuneJob, Long> {
    Optional<FineTuneJob> findByJobId(String jobId);
}
