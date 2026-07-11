package com.example.webhook_worker.consumer;

import com.example.webhook_worker.config.RabbitMQConfig;
import com.example.webhook_worker.service.WebhookDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventConsumer.class);
    
    private final WebhookDeliveryService deliveryService;

    public WebhookEventConsumer(WebhookDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeEvent(String message) {
        logger.info("Received event from RabbitMQ: {}", message);
        deliveryService.processEvent(message);
    }
}
