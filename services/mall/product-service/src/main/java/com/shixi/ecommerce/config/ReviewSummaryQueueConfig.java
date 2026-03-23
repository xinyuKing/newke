package com.shixi.ecommerce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 评价摘要异步队列配置。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Configuration
public class ReviewSummaryQueueConfig {
    public static final String REVIEW_SUMMARY_EXCHANGE = "review.summary";
    public static final String REVIEW_SUMMARY_QUEUE = "review.summary.queue";
    public static final String REVIEW_SUMMARY_ROUTING_KEY = "review.summary";

    @Bean
    public DirectExchange reviewSummaryExchange() {
        return new DirectExchange(REVIEW_SUMMARY_EXCHANGE, true, false);
    }

    @Bean
    public Queue reviewSummaryQueue() {
        return new Queue(REVIEW_SUMMARY_QUEUE, true);
    }

    @Bean
    public Binding reviewSummaryBinding(Queue reviewSummaryQueue, DirectExchange reviewSummaryExchange) {
        return BindingBuilder.bind(reviewSummaryQueue).to(reviewSummaryExchange).with(REVIEW_SUMMARY_ROUTING_KEY);
    }
}
