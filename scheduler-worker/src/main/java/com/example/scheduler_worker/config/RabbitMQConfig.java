package com.example.scheduler_worker.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "live-ops.events";

    @Bean
    public DirectExchange eventsExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
