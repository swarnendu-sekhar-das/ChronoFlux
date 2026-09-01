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
    public static final String ROUTING_KEY_ACTIVE = "event.active";
    public static final String ROUTING_KEY_EXPIRED = "event.expired";

    @Bean
    public DirectExchange eventsExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue webhookQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding activeBinding(Queue webhookQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(webhookQueue).to(eventsExchange).with(ROUTING_KEY_ACTIVE);
    }

    @Bean
    public Binding expiredBinding(Queue webhookQueue, DirectExchange eventsExchange) {
        return BindingBuilder.bind(webhookQueue).to(eventsExchange).with(ROUTING_KEY_EXPIRED);
    }

    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
        org.springframework.amqp.support.converter.Jackson2JsonMessageConverter converter = 
                new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
        
        org.springframework.amqp.support.converter.DefaultClassMapper classMapper = 
                new org.springframework.amqp.support.converter.DefaultClassMapper();
        
        java.util.Map<String, Class<?>> idClassMapping = new java.util.HashMap<>();
        idClassMapping.put("com.example.scheduler_worker.dto.LiveEventActivatedEvent", 
                           com.example.webhook_worker.dto.LiveEventActivatedEvent.class);
        idClassMapping.put("com.example.scheduler_worker.dto.LiveEventExpiredEvent", 
                           com.example.webhook_worker.dto.LiveEventExpiredEvent.class);
        
        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);
        
        return converter;
    }
}
