package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.ProductReviewStats;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewStatsRepository extends JpaRepository<ProductReviewStats, Long> {
    List<ProductReviewStats> findByProductIdIn(List<Long> productIds);

    List<ProductReviewStats> findTop20ByOrderByTotalReviewsDescAvgRatingDesc();

    List<ProductReviewStats> findAllByOrderByTotalReviewsDescAvgRatingDesc(Pageable pageable);
}
