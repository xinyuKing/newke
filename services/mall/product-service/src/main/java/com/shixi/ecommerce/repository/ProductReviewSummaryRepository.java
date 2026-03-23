package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.ProductReviewSummary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewSummaryRepository extends JpaRepository<ProductReviewSummary, Long> {
    Optional<ProductReviewSummary> findByProductId(Long productId);
}
