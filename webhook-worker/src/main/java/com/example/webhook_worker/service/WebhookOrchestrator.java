package com.example.webhook_worker.service;

import com.example.webhook_worker.domain.Webhook;
import com.example.webhook_worker.domain.WebhookDelivery;
import com.example.webhook_worker.dto.LiveEventActivatedEvent;
import com.example.webhook_worker.repository.WebhookDeliveryRepository;
import com.example.webhook_worker.repository.WebhookRepository;
import io.github.resilience4j.retry.annotation.Retry;
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
    private final WebhookHttpClient httpClient;

    public WebhookOrchestrator(WebhookRepository webhookRepository,
                               WebhookDeliveryRepository deliveryRepository,
                               WebhookHttpClient httpClient) {
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
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
            delivery.setTargetUrl(webhook.getTargetUrl());
            delivery.setPayload(event.getPayload());
            delivery.setStatus("ATTEMPTING");
            delivery.setAttemptCount(1);
            delivery.setCreatedAt(ZonedDateTime.now());
            delivery.setLastAttemptAt(ZonedDateTime.now());
            deliveryRepository.save(delivery);

            deliverWithRetry(delivery);
        }
    }

    @Retry(name = "webhookRetry", fallbackMethod = "markAsFailed")
    public void deliverWithRetry(WebhookDelivery delivery) {
        logger.info("Delivering to URL: {}", delivery.getTargetUrl());
        httpClient.sendPayload(delivery.getTargetUrl(), delivery.getPayload());

        // If we reach here, the HTTP call succeeded
        delivery.setStatus("SUCCESS");
        delivery.setResponseCode(200);
        delivery.setLastAttemptAt(ZonedDateTime.now());
        deliveryRepository.save(delivery);
        logger.info("Delivery {} successful", delivery.getId());
    }

    /**
     * Resilience4j fallback — called after all retry attempts are exhausted.
     */
    public void markAsFailed(WebhookDelivery delivery, Exception e) {
        logger.error("Delivery {} failed after all retries: {}", delivery.getId(), e.getMessage());
        delivery.setStatus("FAILED");
        delivery.setFailureReason(e.getMessage());
        delivery.setLastAttemptAt(ZonedDateTime.now());
        deliveryRepository.save(delivery);
    }
}
