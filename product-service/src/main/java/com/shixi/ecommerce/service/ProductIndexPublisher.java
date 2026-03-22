package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.ProductIndexQueueConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 商品索引消息发布器，保证事务提交后再投递。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class ProductIndexPublisher {
    private final RabbitTemplate rabbitTemplate;

    public ProductIndexPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 事务提交后发布商品索引刷新消息。
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
                ProductIndexQueueConfig.PRODUCT_INDEX_EXCHANGE,
                ProductIndexQueueConfig.PRODUCT_INDEX_ROUTING_KEY,
                String.valueOf(productId));
    }
}
