package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.ProductReviewStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewStatsRepository extends JpaRepository<ProductReviewStats, Long> {
    List<ProductReviewStats> findByProductIdIn(List<Long> productIds);

    List<ProductReviewStats> findTop20ByOrderByTotalReviewsDescAvgRatingDesc();
}
