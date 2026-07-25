package com.example.webhook_worker.service;

import com.example.webhook_worker.domain.Webhook;
import com.example.webhook_worker.domain.WebhookDelivery;
import com.example.webhook_worker.dto.LiveEventActivatedEvent;
import com.example.webhook_worker.repository.WebhookDeliveryRepository;
import com.example.webhook_worker.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(WebhookOrchestrator.class);

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final com.example.webhook_worker.repository.FailedWebhookRepository failedWebhookRepository;
    private final WebhookHttpClient httpClient;

    public WebhookOrchestrator(WebhookRepository webhookRepository, WebhookDeliveryRepository deliveryRepository, com.example.webhook_worker.repository.FailedWebhookRepository failedWebhookRepository, WebhookHttpClient httpClient) {
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
        this.failedWebhookRepository = failedWebhookRepository;
        this.httpClient = httpClient;
    }

    public void processEvent(LiveEventActivatedEvent event) {
        logger.info("Processing webhook delivery for event ID: {}", event.getEventId());
        
        List<Webhook> webhooks = webhookRepository.findBySchemaIdAndIsActiveTrue(event.getSchemaId());
        
        for (Webhook webhook : webhooks) {
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setId(UUID.randomUUID());
            delivery.setWebhookId(webhook.getId());
            delivery.setEventId(event.getEventId());
            delivery.setStatus("ATTEMPTING");
            delivery.setAttemptCount(1);
            delivery.setCreatedAt(ZonedDateTime.now());
            delivery.setLastAttemptAt(ZonedDateTime.now());
            deliveryRepository.save(delivery);

            logger.info("Attempt 1 delivering to URL: {}", webhook.getTargetUrl());

            httpClient.sendPayload(webhook.getTargetUrl(), event.getPayload())
                .subscribe(
                    null, // onNext is never called for Mono<Void>
                    error -> {
                        logger.error("Delivery {} failed: {}", delivery.getId(), error.getMessage());
                        delivery.setStatus("FAILED");
                        deliveryRepository.save(delivery);
                        failedWebhookRepository.save(new com.example.webhook_worker.entity.FailedWebhook(event.getEventId(), event.getPayload(), error.getMessage()));
                    },
                    () -> { // onComplete
                        logger.info("Delivery {} successful", delivery.getId());
                        delivery.setStatus("SUCCESS");
                        delivery.setResponseCode(200);
                        deliveryRepository.save(delivery);
                    }
                );
        }
    }
}
