package com.shixi.ecommerce.repository;

import com.shixi.ecommerce.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    long countByProductId(Long productId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    List<Review> findTop50ByProductIdOrderByCreatedAtDesc(Long productId);

    Slice<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Slice<Review> findByProductIdAndRatingOrderByCreatedAtDesc(Long productId, Integer rating, Pageable pageable);

    @Query("select r from Review r where r.productId = :productId and " +
            "(:rating is null or r.rating = :rating) and " +
            "(:cursorTime is null or r.createdAt < :cursorTime " +
            "or (r.createdAt = :cursorTime and r.id < :cursorId)) " +
            "order by r.createdAt desc, r.id desc")
    List<Review> findByProductIdCursor(@Param("productId") Long productId,
                                       @Param("rating") Integer rating,
                                       @Param("cursorTime") java.time.LocalDateTime cursorTime,
                                       @Param("cursorId") Long cursorId,
                                       Pageable pageable);

    @Query("select r.productId as productId, count(r) as total, " +
            "sum(case when r.rating <= 2 then 1 else 0 end) as negative, " +
            "avg(r.rating) as avgRating " +
            "from Review r group by r.productId")
    List<ReviewStats> getReviewStats();

    @Query("select r.productId as productId, count(r) as total, " +
            "sum(case when r.rating <= 2 then 1 else 0 end) as negative, " +
            "avg(r.rating) as avgRating " +
            "from Review r where r.productId in :productIds group by r.productId")
    List<ReviewStats> getReviewStatsByProductIds(@Param("productIds") List<Long> productIds);

    @Query("select r from Review r where r.productId = :productId and r.rating <= 2 order by r.createdAt desc")
    List<Review> findNegativeReviews(@Param("productId") Long productId);

    interface ReviewStats {
        Long getProductId();
        Long getTotal();
        Long getNegative();
        Double getAvgRating();
    }
}
