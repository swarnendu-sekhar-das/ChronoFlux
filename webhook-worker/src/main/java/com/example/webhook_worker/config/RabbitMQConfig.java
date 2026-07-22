package com.example.webhook_worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "live-ops.events";
    public static final String QUEUE_NAME = "live-ops.events.webhook-delivery";
    public static final String ROUTING_KEY = "event.active";
    @Bean
    public DirectExchange eventsExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue webhookQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding binding(Queue webhookQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(webhookQueue).to(eventsExchange).with(ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
        return new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
    }
}
