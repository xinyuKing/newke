package com.shixi.ecommerce.service;

import com.shixi.ecommerce.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * 订单事件监听器，处理超时取消等异步逻辑。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Service
public class OrderEventListener {
    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final OrderService orderService;

    public OrderEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 订单创建事件。
     *
     * @param orderNo 订单号
     */
    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(String orderNo) {
        log.info("Order created event received: {}", orderNo);
    }

    /**
     * 订单超时事件，尝试取消并释放库存。
     *
     * @param orderNo 订单号
     */
    @RabbitListener(queues = RabbitConfig.ORDER_TIMEOUT_QUEUE)
    public void onOrderTimeout(String orderNo) {
        boolean canceled = orderService.cancelIfUnpaid(orderNo);
        log.info("Order timeout handled: {}, canceled={}", orderNo, canceled);
    }

    /**
     * 订单支付事件。
     *
     * @param orderNo 订单号
     */
    @RabbitListener(queues = RabbitConfig.ORDER_PAID_QUEUE)
    public void onOrderPaid(String orderNo) {
        log.info("Order paid event received: {}", orderNo);
    }

    /**
     * 订单发货事件。
     *
     * @param orderNo 订单号
     */
    @RabbitListener(queues = RabbitConfig.ORDER_SHIPPED_QUEUE)
    public void onOrderShipped(String orderNo) {
        log.info("Order shipped event received: {}", orderNo);
    }

    /**
     * 订单完成事件。
     *
     * @param orderNo 订单号
     */
    @RabbitListener(queues = RabbitConfig.ORDER_COMPLETED_QUEUE)
    public void onOrderCompleted(String orderNo) {
        log.info("Order completed event received: {}", orderNo);
    }
}
