package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.ReviewStatsQueueConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 评价统计消息发布器，保证事务提交后再投递。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ReviewStatsPublisher {
    private final RabbitTemplate rabbitTemplate;

    public ReviewStatsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 事务提交后发布统计刷新消息。
     *
     * @param productId 商品 ID
     */
    public void publishAfterCommit(Long productId) {
        if (productId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(productId);
                }
            });
            return;
        }
        publish(productId);
    }

    private void publish(Long productId) {
        rabbitTemplate.convertAndSend(
                ReviewStatsQueueConfig.REVIEW_STATS_EXCHANGE,
                ReviewStatsQueueConfig.REVIEW_STATS_ROUTING_KEY,
                String.valueOf(productId));
    }
}
