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
    
    public static final String DLX_NAME = "live-ops.events.dlx";
    public static final String DLQ_NAME = "live-ops.events.webhook-delivery.dlq";

    @Bean
    public DirectExchange eventsExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue webhookQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding binding(Queue webhookQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(webhookQueue).to(eventsExchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(ROUTING_KEY);
    }

    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
        return new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
    }
}
