package com.example.webhook_worker.consumer;

import com.example.webhook_worker.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeEvent(String message) {
        logger.info("Received event from RabbitMQ: {}", message);
        
        // TODO: In Phase 4, we will parse this message, find the webhook subscriptions 
        // in the database, and execute the actual HTTP POST requests.
        logger.info("Simulating Webhook Delivery for event payload...");
    }
}
