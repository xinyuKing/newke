package com.shixi.ecommerce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 商品索引异步队列配置。
 *
 * @author shixi
 * @date 2026-03-20
 */
@Configuration
public class ProductIndexQueueConfig {
    public static final String PRODUCT_INDEX_EXCHANGE = "product.index";
    public static final String PRODUCT_INDEX_QUEUE = "product.index.queue";
    public static final String PRODUCT_INDEX_ROUTING_KEY = "product.index";

    @Bean
    public DirectExchange productIndexExchange() {
        return new DirectExchange(PRODUCT_INDEX_EXCHANGE, true, false);
    }

    @Bean
    public Queue productIndexQueue() {
        return new Queue(PRODUCT_INDEX_QUEUE, true);
    }

    @Bean
    public Binding productIndexBinding(Queue productIndexQueue, DirectExchange productIndexExchange) {
        return BindingBuilder.bind(productIndexQueue).to(productIndexExchange).with(PRODUCT_INDEX_ROUTING_KEY);
    }
}
