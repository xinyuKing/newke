package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.ProductReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductReviewSummaryRepository extends JpaRepository<ProductReviewSummary, Long> {
    Optional<ProductReviewSummary> findByProductId(Long productId);
}
