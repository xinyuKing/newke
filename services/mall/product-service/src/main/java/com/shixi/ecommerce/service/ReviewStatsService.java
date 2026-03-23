package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.ProductReviewStats;
import com.shixi.ecommerce.repository.ProductReviewStatsRepository;
import com.shixi.ecommerce.repository.ReviewRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评价统计聚合服务，异步刷新商品评价统计快照。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ReviewStatsService {
    private static final Logger log = LoggerFactory.getLogger(ReviewStatsService.class);

    private final ReviewRepository reviewRepository;
    private final ProductReviewStatsRepository statsRepository;

    public ReviewStatsService(ReviewRepository reviewRepository, ProductReviewStatsRepository statsRepository) {
        this.reviewRepository = reviewRepository;
        this.statsRepository = statsRepository;
    }

    /**
     * 刷新指定商品的统计快照。
     *
     * @param productId 商品 ID
     */
    @Transactional
    public void refreshStats(Long productId) {
        if (productId == null) {
            return;
        }
        List<ReviewRepository.ReviewStats> statsList = reviewRepository.getReviewStatsByProductIds(List.of(productId));
        ProductReviewStats stats = statsRepository.findById(productId).orElseGet(ProductReviewStats::new);
        stats.setProductId(productId);
        if (statsList.isEmpty()) {
            stats.setTotalReviews(0L);
            stats.setNegativeReviews(0L);
            stats.setAvgRating(0.0);
        } else {
            ReviewRepository.ReviewStats snapshot = statsList.get(0);
            stats.setTotalReviews(snapshot.getTotal() == null ? 0L : snapshot.getTotal());
            stats.setNegativeReviews(snapshot.getNegative() == null ? 0L : snapshot.getNegative());
            stats.setAvgRating(snapshot.getAvgRating() == null ? 0.0 : snapshot.getAvgRating());
        }
        statsRepository.save(stats);
    }

    /**
     * 批量刷新统计快照。
     *
     * @param productIds 商品 ID 列表
     */
    @Transactional
    public void refreshStatsBatch(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        for (Long productId : productIds) {
            try {
                refreshStats(productId);
            } catch (Exception ex) {
                log.warn("Refresh review stats failed, productId={}", productId, ex);
            }
        }
    }
}
