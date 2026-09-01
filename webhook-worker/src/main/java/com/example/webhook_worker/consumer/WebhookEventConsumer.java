package com.example.webhook_worker.consumer;

import com.example.webhook_worker.config.RabbitMQConfig;
import com.example.webhook_worker.service.WebhookOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.example.webhook_worker.dto.LiveEventActivatedEvent;
import com.example.webhook_worker.dto.LiveEventExpiredEvent;
import org.springframework.stereotype.Component;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;

@Component
@RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
public class WebhookEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventConsumer.class);
    
    private final WebhookOrchestrator orchestrator;

    public WebhookEventConsumer(WebhookOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @RabbitHandler
    public void consumeEvent(LiveEventActivatedEvent event) {
        logger.info("Received ACTIVATED event from RabbitMQ for Event ID: {}", event.getEventId());
        orchestrator.processEvent(event);
    }

    @RabbitHandler
    public void consumeExpirationEvent(LiveEventExpiredEvent event) {
        logger.info("Received EXPIRED event from RabbitMQ for Event ID: {}", event.getEventId());
        orchestrator.processExpirationEvent(event);
    }
}
