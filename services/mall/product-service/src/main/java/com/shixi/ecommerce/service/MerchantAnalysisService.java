package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.Product;
import com.shixi.ecommerce.domain.ProductReviewStats;
import com.shixi.ecommerce.domain.ProductStatus;
import com.shixi.ecommerce.dto.MerchantAnalysisResponse;
import com.shixi.ecommerce.repository.ProductRepository;
import com.shixi.ecommerce.repository.ProductReviewStatsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商家经营分析服务，基于评价统计快照输出风险提示。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class MerchantAnalysisService {
    private static final long MIN_RISK_REVIEWS = 5;
    private static final double NEGATIVE_RATIO_THRESHOLD = 0.30;

    private final ProductRepository productRepository;
    private final ProductReviewStatsRepository reviewStatsRepository;

    public MerchantAnalysisService(ProductRepository productRepository,
                                   ProductReviewStatsRepository reviewStatsRepository) {
        this.productRepository = productRepository;
        this.reviewStatsRepository = reviewStatsRepository;
    }

    @Cacheable(cacheNames = "merchantAnalysis", key = "#merchantId")
    @Transactional(readOnly = true)
    public MerchantAnalysisResponse analyze(Long merchantId) {
        List<Product> products = productRepository.findByMerchantId(merchantId);
        long productCount = products.size();
        long activeProductCount = products.stream()
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .count();

        if (products.isEmpty()) {
            return new MerchantAnalysisResponse(
                    merchantId,
                    0,
                    0,
                    0,
                    0.0,
                    Collections.emptyList(),
                    "No products yet."
            );
        }

        List<Long> productIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        List<ProductReviewStats> stats = reviewStatsRepository.findByProductIdIn(productIds);

        long totalReviews = stats.stream()
                .mapToLong(stat -> stat.getTotalReviews() == null ? 0L : stat.getTotalReviews())
                .sum();
        double avgRating = 0.0;
        if (totalReviews > 0) {
            double weightedSum = stats.stream()
                    .mapToDouble(stat -> safeAvg(stat) * stat.getTotalReviews())
                    .sum();
            avgRating = weightedSum / totalReviews;
        }

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<String> riskProducts = stats.stream()
                .filter(stat -> stat.getTotalReviews() != null && stat.getTotalReviews() >= MIN_RISK_REVIEWS)
                .filter(stat -> stat.getNegativeReviews() != null && stat.getNegativeReviews() > 0)
                .filter(stat -> negativeRatio(stat) >= NEGATIVE_RATIO_THRESHOLD)
                .sorted(Comparator.comparingDouble(this::negativeRatio).reversed())
                .limit(5)
                .map(stat -> {
                    Product product = productMap.get(stat.getProductId());
                    String name = product == null ? "unknown" : product.getName();
                    return String.format("%s(#%d) negRatio=%.2f", name, stat.getProductId(), negativeRatio(stat));
                })
                .collect(Collectors.toList());

        String summary = String.format(
                "Merchant %d: products=%d, active=%d, reviews=%d, avg=%.2f, risk=%d.",
                merchantId,
                productCount,
                activeProductCount,
                totalReviews,
                avgRating,
                riskProducts.size()
        );

        return new MerchantAnalysisResponse(
                merchantId,
                productCount,
                activeProductCount,
                totalReviews,
                avgRating,
                riskProducts,
                summary
        );
    }

    private double safeAvg(ProductReviewStats stats) {
        return stats.getAvgRating() == null ? 0.0 : stats.getAvgRating();
    }

    private double negativeRatio(ProductReviewStats stats) {
        if (stats.getTotalReviews() == null || stats.getTotalReviews() == 0) {
            return 0.0;
        }
        long negative = stats.getNegativeReviews() == null ? 0L : stats.getNegativeReviews();
        return (double) negative / stats.getTotalReviews();
    }
}
