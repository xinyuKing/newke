package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.ReviewStatsQueueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * 评价统计消息监听器。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ReviewStatsListener {
    private static final Logger log = LoggerFactory.getLogger(ReviewStatsListener.class);

    private final ReviewStatsService reviewStatsService;

    public ReviewStatsListener(ReviewStatsService reviewStatsService) {
        this.reviewStatsService = reviewStatsService;
    }

    @RabbitListener(queues = ReviewStatsQueueConfig.REVIEW_STATS_QUEUE)
    public void onMessage(String productIdText) {
        if (productIdText == null || productIdText.isBlank()) {
            return;
        }
        try {
            Long productId = Long.valueOf(productIdText);
            reviewStatsService.refreshStats(productId);
        } catch (Exception ex) {
            log.warn("Handle review stats message failed: {}", productIdText, ex);
        }
    }
}
