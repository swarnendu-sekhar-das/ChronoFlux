package com.example.webhook_worker.consumer;

import com.example.webhook_worker.config.RabbitMQConfig;
import com.example.webhook_worker.service.WebhookDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.example.webhook_worker.dto.LiveEventActivatedEvent;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventConsumer.class);
    
    private final WebhookDeliveryService deliveryService;

    public WebhookEventConsumer(WebhookDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeEvent(LiveEventActivatedEvent event) {
        logger.info("Received event from RabbitMQ for Event ID: {}", event.getEventId());
        deliveryService.processEvent(event);
    }
}
