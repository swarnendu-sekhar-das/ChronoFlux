package com.example.webhook_worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "events.exchange";
    public static final String QUEUE_NAME = "webhook.events.queue";
    public static final String ROUTING_KEY = "event.active";

    @Bean
    public DirectExchange eventsExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue webhookQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue webhookQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(webhookQueue).to(eventsExchange).with(ROUTING_KEY);
    }
}
