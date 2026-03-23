package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.ReviewSummaryQueueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * 评价摘要消息监听器，异步生成摘要以降低写入延迟。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ReviewSummaryListener {
    private static final Logger log = LoggerFactory.getLogger(ReviewSummaryListener.class);

    private final ReviewSummaryService reviewSummaryService;

    public ReviewSummaryListener(ReviewSummaryService reviewSummaryService) {
        this.reviewSummaryService = reviewSummaryService;
    }

    /**
     * 处理商品评价摘要生成任务。
     *
     * @param productId 商品 ID（字符串）
     */
    @RabbitListener(queues = ReviewSummaryQueueConfig.REVIEW_SUMMARY_QUEUE)
    public void handle(String productId) {
        if (productId == null || productId.isBlank()) {
            return;
        }
        try {
            Long id = Long.parseLong(productId.trim());
            reviewSummaryService.updateSummaryIfNeeded(id);
        } catch (NumberFormatException ex) {
            log.warn("Invalid productId in review summary message: {}", productId);
        }
    }
}
