package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductReviewStats;
import com.shixi.ecommerce.domain.Review;
import com.shixi.ecommerce.dto.AdminRiskProductResponse;
import com.shixi.ecommerce.repository.ProductRepository;
import com.shixi.ecommerce.repository.ProductReviewStatsRepository;
import com.shixi.ecommerce.repository.ReviewRepository;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端评价风控服务，基于统计快照识别高风险商品。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class AdminReviewService {
    private static final long MIN_RISK_REVIEWS = 5;
    private static final double NEGATIVE_RATIO_THRESHOLD = 0.30;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ReviewSummaryBuilder summaryBuilder;
    private final ProductReviewStatsRepository statsRepository;

    public AdminReviewService(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            ReviewSummaryBuilder summaryBuilder,
            ProductReviewStatsRepository statsRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.summaryBuilder = summaryBuilder;
        this.statsRepository = statsRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminRiskProductResponse> listRiskProducts() {
        List<ProductReviewStats> stats = statsRepository.findAll();
        if (stats.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductReviewStats> riskStats = stats.stream()
                .filter(stat -> stat.getTotalReviews() != null && stat.getTotalReviews() >= MIN_RISK_REVIEWS)
                .filter(stat -> stat.getNegativeReviews() != null && stat.getNegativeReviews() > 0)
                .filter(stat -> negativeRatio(stat) >= NEGATIVE_RATIO_THRESHOLD)
                .sorted(Comparator.comparingDouble(this::negativeRatio).reversed())
                .collect(Collectors.toList());

        if (riskStats.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds =
                riskStats.stream().map(ProductReviewStats::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return riskStats.stream()
                .map(stat -> {
                    Product product = productMap.get(stat.getProductId());
                    long total = stat.getTotalReviews() == null ? 0L : stat.getTotalReviews();
                    long negative = stat.getNegativeReviews() == null ? 0L : stat.getNegativeReviews();
                    double ratio = total == 0 ? 0.0 : (double) negative / total;
                    List<Review> reviews =
                            reviewRepository.findTop50ByProductIdOrderByCreatedAtDesc(stat.getProductId());
                    String summary = buildSummary(stat, ratio, reviews);
                    return new AdminRiskProductResponse(
                            stat.getProductId(),
                            product == null ? null : product.getMerchantId(),
                            product == null ? "unknown" : product.getName(),
                            total,
                            negative,
                            ratio,
                            summary);
                })
                .collect(Collectors.toList());
    }

    private String buildSummary(ProductReviewStats stats, double ratio, List<Review> reviews) {
        double avgRating = stats.getAvgRating() == null ? 0.0 : stats.getAvgRating();
        String base = summaryBuilder.buildSummary(stats.getProductId(), reviews);
        return String.format("negRatio=%.2f, avg=%.2f. %s", ratio, avgRating, base);
    }

    private double negativeRatio(ProductReviewStats stats) {
        if (stats.getTotalReviews() == null || stats.getTotalReviews() == 0) {
            return 0.0;
        }
        long negative = stats.getNegativeReviews() == null ? 0L : stats.getNegativeReviews();
        return (double) negative / stats.getTotalReviews();
    }
}
