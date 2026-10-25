package com.example.webhook_worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import java.util.HashMap;
import java.util.Map;
import com.example.webhook_worker.dto.LiveEventActivatedEvent;
import com.example.webhook_worker.dto.LiveEventExpiredEvent;
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
        return QueueBuilder.durable(QUEUE_NAME).build();
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
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        
        DefaultClassMapper classMapper = new DefaultClassMapper();
        
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.example.scheduler_worker.dto.LiveEventActivatedEvent", 
                           LiveEventActivatedEvent.class);
        idClassMapping.put("com.example.scheduler_worker.dto.LiveEventExpiredEvent", 
                           LiveEventExpiredEvent.class);
        
        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);
        
        return converter;
    }
}
