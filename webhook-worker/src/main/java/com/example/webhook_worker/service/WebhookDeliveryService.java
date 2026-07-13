package com.example.webhook_worker.service;

import com.example.webhook_worker.domain.Webhook;
import com.example.webhook_worker.domain.WebhookDelivery;
import com.example.webhook_worker.dto.LiveEventActivatedEvent;
import com.example.webhook_worker.repository.WebhookDeliveryRepository;
import com.example.webhook_worker.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryService.class);

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebClient webClient;

    public WebhookDeliveryService(WebhookRepository webhookRepository, WebhookDeliveryRepository deliveryRepository, WebClient webClient) {
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
        this.webClient = webClient;
    }

    public void processEvent(LiveEventActivatedEvent event) {
        logger.info("Processing webhook delivery for event ID: {}", event.getEventId());
        
        List<Webhook> webhooks = webhookRepository.findBySchemaIdAndIsActiveTrue(event.getSchemaId());
        
        for (Webhook webhook : webhooks) {
            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setId(UUID.randomUUID());
            delivery.setWebhookId(webhook.getId());
            delivery.setEventId(event.getEventId());
            delivery.setStatus("PENDING");
            delivery.setAttemptCount(0);
            delivery.setCreatedAt(ZonedDateTime.now());
            deliveryRepository.save(delivery);

            try {
                deliverWithRetry(delivery, event.getPayload(), webhook.getTargetUrl());
            } catch (Exception e) {
                logger.error("Webhook delivery failed after retries for delivery ID: {}", delivery.getId());
                // Rethrow so RabbitMQ can DLQ it if needed
                throw new RuntimeException("Webhook delivery failed", e);
            }
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void deliverWithRetry(WebhookDelivery delivery, String payload, String targetUrl) {
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(ZonedDateTime.now());
        delivery.setStatus("RETRYING");
        deliveryRepository.save(delivery);
        
        logger.info("Attempt {} delivering to URL: {}", delivery.getAttemptCount(), targetUrl);

        try {
            webClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block(); // Block since we are in a message listener thread
            
            delivery.setStatus("SUCCESS");
            delivery.setResponseCode(200);
            deliveryRepository.save(delivery);
            logger.info("Delivery {} successful", delivery.getId());
        } catch (Exception e) {
            logger.error("Delivery {} failed: {}", delivery.getId(), e.getMessage());
            if (delivery.getAttemptCount() >= 3) {
                delivery.setStatus("FAILED");
                deliveryRepository.save(delivery);
            }
            throw e; // Trigger retry
        }
    }
}
