package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 订单事件发布器，确保事务提交后再投递消息。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public OrderEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 订单创建事件，同时投递延迟检查消息。
     *
     * @param orderNo 订单号
     */
    public void publishOrderCreated(String orderNo) {
        publishAfterCommit(() -> {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.ORDER_EVENTS_EXCHANGE, RabbitConfig.ROUTING_ORDER_CREATED, orderNo);
            rabbitTemplate.convertAndSend(RabbitConfig.ORDER_DELAY_EXCHANGE, RabbitConfig.ROUTING_ORDER_DELAY, orderNo);
        });
    }

    /**
     * 订单支付事件。
     *
     * @param orderNo 订单号
     */
    public void publishOrderPaid(String orderNo) {
        publishAfterCommit(() -> rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EVENTS_EXCHANGE, RabbitConfig.ROUTING_ORDER_PAID, orderNo));
    }

    /**
     * 订单发货事件。
     *
     * @param orderNo 订单号
     */
    public void publishOrderShipped(String orderNo) {
        publishAfterCommit(() -> rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EVENTS_EXCHANGE, RabbitConfig.ROUTING_ORDER_SHIPPED, orderNo));
    }

    /**
     * 订单完成事件。
     *
     * @param orderNo 订单号
     */
    public void publishOrderCompleted(String orderNo) {
        publishAfterCommit(() -> rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EVENTS_EXCHANGE, RabbitConfig.ROUTING_ORDER_COMPLETED, orderNo));
    }

    private void publishAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }
}
