package com.shixi.ecommerce.service;

import com.shixi.ecommerce.domain.ProductReviewSummary;
import com.shixi.ecommerce.domain.Review;
import com.shixi.ecommerce.dto.ReviewSummaryResponse;
import com.shixi.ecommerce.repository.ProductReviewSummaryRepository;
import com.shixi.ecommerce.repository.ReviewRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评价摘要服务，提供摘要生成与缓存读取。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ReviewSummaryService {
    private final ReviewRepository reviewRepository;
    private final ProductReviewSummaryRepository summaryRepository;
    private final ReviewSummaryBuilder summaryBuilder;

    public ReviewSummaryService(ReviewRepository reviewRepository,
                                ProductReviewSummaryRepository summaryRepository,
                                ReviewSummaryBuilder summaryBuilder) {
        this.reviewRepository = reviewRepository;
        this.summaryRepository = summaryRepository;
        this.summaryBuilder = summaryBuilder;
    }

    /**
     * 达到阈值时异步更新摘要，减少频繁更新带来的压力。
     *
     * @param productId 商品 ID
     */
    @Transactional
    public void updateSummaryIfNeeded(Long productId) {
        long total = reviewRepository.countByProductId(productId);
        if (total == 0 || total % 10 != 0) {
            return;
        }
        List<Review> reviews = reviewRepository.findTop50ByProductIdOrderByCreatedAtDesc(productId);
        String summary = summaryBuilder.buildSummary(productId, reviews);
        ProductReviewSummary entity = summaryRepository.findByProductId(productId).orElseGet(ProductReviewSummary::new);
        entity.setProductId(productId);
        entity.setReviewCount(total);
        entity.setSummary(summary);
        summaryRepository.save(entity);
    }

    /**
     * 读取评价摘要，缓存未命中时生成并落库。
     *
     * @param productId 商品 ID
     * @return 评价摘要
     */
    @Cacheable(cacheNames = "reviewSummary", key = "#productId")
    @Transactional
    public ReviewSummaryResponse getSummary(Long productId) {
        return summaryRepository.findByProductId(productId)
                .map(entity -> new ReviewSummaryResponse(entity.getProductId(), entity.getReviewCount(), entity.getSummary()))
                .orElseGet(() -> {
                    long total = reviewRepository.countByProductId(productId);
                    List<Review> reviews = reviewRepository.findTop50ByProductIdOrderByCreatedAtDesc(productId);
                    String summary = summaryBuilder.buildSummary(productId, reviews);
                    ProductReviewSummary entity = new ProductReviewSummary();
                    entity.setProductId(productId);
                    entity.setReviewCount(total);
                    entity.setSummary(summary);
                    try {
                        summaryRepository.save(entity);
                    } catch (DataIntegrityViolationException ex) {
                        return summaryRepository.findByProductId(productId)
                                .map(existing -> new ReviewSummaryResponse(existing.getProductId(), existing.getReviewCount(), existing.getSummary()))
                                .orElseGet(() -> new ReviewSummaryResponse(productId, total, summary));
                    }
                    return new ReviewSummaryResponse(productId, total, summary);
                });
    }
}
