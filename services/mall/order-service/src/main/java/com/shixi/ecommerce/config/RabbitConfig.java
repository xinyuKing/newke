package com.shixi.ecommerce.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String ORDER_EVENTS_EXCHANGE = "order.events";
    public static final String ORDER_DELAY_EXCHANGE = "order.delay";

    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_PAID_QUEUE = "order.paid.queue";
    public static final String ORDER_SHIPPED_QUEUE = "order.shipped.queue";
    public static final String ORDER_COMPLETED_QUEUE = "order.completed.queue";

    public static final String ROUTING_ORDER_CREATED = "order.created";
    public static final String ROUTING_ORDER_TIMEOUT = "order.timeout";
    public static final String ROUTING_ORDER_DELAY = "order.delay";
    public static final String ROUTING_ORDER_PAID = "order.paid";
    public static final String ROUTING_ORDER_SHIPPED = "order.shipped";
    public static final String ROUTING_ORDER_COMPLETED = "order.completed";

    @Bean
    public TopicExchange orderEventsExchange() {
        return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true);
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return new Queue(ORDER_TIMEOUT_QUEUE, true);
    }

    @Bean
    public Queue orderPaidQueue() {
        return new Queue(ORDER_PAID_QUEUE, true);
    }

    @Bean
    public Queue orderShippedQueue() {
        return new Queue(ORDER_SHIPPED_QUEUE, true);
    }

    @Bean
    public Queue orderCompletedQueue() {
        return new Queue(ORDER_COMPLETED_QUEUE, true);
    }

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 15 * 60 * 1000);
        args.put("x-dead-letter-exchange", ORDER_EVENTS_EXCHANGE);
        args.put("x-dead-letter-routing-key", ROUTING_ORDER_TIMEOUT);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(orderEventsExchange).with(ROUTING_ORDER_CREATED);
    }

    @Bean
    public Binding orderTimeoutBinding(Queue orderTimeoutQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderTimeoutQueue).to(orderEventsExchange).with(ROUTING_ORDER_TIMEOUT);
    }

    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(orderEventsExchange).with(ROUTING_ORDER_PAID);
    }

    @Bean
    public Binding orderShippedBinding(Queue orderShippedQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderShippedQueue).to(orderEventsExchange).with(ROUTING_ORDER_SHIPPED);
    }

    @Bean
    public Binding orderCompletedBinding(Queue orderCompletedQueue, TopicExchange orderEventsExchange) {
        return BindingBuilder.bind(orderCompletedQueue).to(orderEventsExchange).with(ROUTING_ORDER_COMPLETED);
    }

    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, DirectExchange orderDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue).to(orderDelayExchange).with(ROUTING_ORDER_DELAY);
    }
}
