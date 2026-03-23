package com.shixi.ecommerce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 评价统计异步队列配置。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Configuration
public class ReviewStatsQueueConfig {
    public static final String REVIEW_STATS_EXCHANGE = "review.stats";
    public static final String REVIEW_STATS_QUEUE = "review.stats.queue";
    public static final String REVIEW_STATS_ROUTING_KEY = "review.stats";

    @Bean
    public DirectExchange reviewStatsExchange() {
        return new DirectExchange(REVIEW_STATS_EXCHANGE, true, false);
    }

    @Bean
    public Queue reviewStatsQueue() {
        return new Queue(REVIEW_STATS_QUEUE, true);
    }

    @Bean
    public Binding reviewStatsBinding(Queue reviewStatsQueue, DirectExchange reviewStatsExchange) {
        return BindingBuilder.bind(reviewStatsQueue)
                .to(reviewStatsExchange)
                .with(REVIEW_STATS_ROUTING_KEY);
    }
}
