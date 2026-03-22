package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.ReviewSummaryQueueConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 评价摘要消息发布器，保证事务提交后再投递。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ReviewSummaryPublisher {
    private final RabbitTemplate rabbitTemplate;

    public ReviewSummaryPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 事务提交后发布摘要刷新消息。
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
                ReviewSummaryQueueConfig.REVIEW_SUMMARY_EXCHANGE,
                ReviewSummaryQueueConfig.REVIEW_SUMMARY_ROUTING_KEY,
                String.valueOf(productId));
    }
}
