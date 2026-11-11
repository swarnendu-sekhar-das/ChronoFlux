package com.example.webhook_worker.service;

import com.example.webhook_worker.domain.Webhook;
import com.example.webhook_worker.domain.WebhookDelivery;
import com.example.webhook_worker.dto.LiveEventActivatedEvent;
import com.example.webhook_worker.dto.LiveEventExpiredEvent;
import com.example.webhook_worker.repository.WebhookDeliveryRepository;
import com.example.webhook_worker.repository.WebhookRepository;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final WebhookDispatcherService dispatcherService;
    private final ObjectMapper objectMapper;

    public WebhookOrchestrator(WebhookRepository webhookRepository,
                               WebhookDeliveryRepository deliveryRepository,
                               WebhookDispatcherService dispatcherService,
                               ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
        this.dispatcherService = dispatcherService;
        this.objectMapper = objectMapper;
    }

    private static class WebhookEnvelope {
        public UUID eventId;
        public String eventType;
        @JsonRawValue
        public String data;

        public WebhookEnvelope(UUID eventId, String eventType, String data) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.data = data;
        }
    }

    public void processEvent(LiveEventActivatedEvent event) {
        logger.info("Processing webhook delivery for activated event ID: {}", event.getEventId());

        List<Webhook> webhooks = webhookRepository.findBySchemaIdAndIsActiveTrue(event.getSchemaId());

        for (Webhook webhook : webhooks) {
            // Consumer Idempotency Check: prevent duplicate delivery on RabbitMQ redelivery
            if (deliveryRepository.existsByWebhookIdAndEventIdAndEventType(webhook.getId(), event.getEventId(), "ACTIVATED")) {
                logger.info("Delivery already exists for webhook {} and event {} (ACTIVATED). Skipping duplicate.",
                        webhook.getId(), event.getEventId());
                continue;
            }

            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setId(UUID.randomUUID());
            delivery.setWebhookId(webhook.getId());
            delivery.setEventId(event.getEventId());
            delivery.setEventType("ACTIVATED");
            delivery.setTargetUrl(webhook.getTargetUrl());
            
            try {
                WebhookEnvelope envelope = new WebhookEnvelope(event.getEventId(), "ACTIVATED", event.getPayload());
                delivery.setPayload(objectMapper.writeValueAsString(envelope));
            } catch (Exception e) {
                logger.error("Failed to serialize webhook envelope", e);
                continue;
            }

            delivery.setStatus("ATTEMPTING");
            delivery.setAttemptCount(1);
            delivery.setCreatedAt(ZonedDateTime.now());
            delivery.setLastAttemptAt(ZonedDateTime.now());
            deliveryRepository.save(delivery);

            // Delegated to WebhookDispatcherService to ensure Spring @Retry AOP proxy intercepts
            dispatcherService.deliverWithRetry(delivery);
        }
    }

    public void processExpirationEvent(LiveEventExpiredEvent event) {
        logger.info("Processing webhook delivery for expired event ID: {}", event.getEventId());

        List<Webhook> webhooks = webhookRepository.findBySchemaIdAndIsActiveTrue(event.getSchemaId());

        for (Webhook webhook : webhooks) {
            // Consumer Idempotency Check: prevent duplicate delivery for EXPIRED lifecycle
            if (deliveryRepository.existsByWebhookIdAndEventIdAndEventType(webhook.getId(), event.getEventId(), "EXPIRED")) {
                logger.info("Delivery already exists for webhook {} and event {} (EXPIRED). Skipping duplicate.",
                        webhook.getId(), event.getEventId());
                continue;
            }

            WebhookDelivery delivery = new WebhookDelivery();
            delivery.setId(UUID.randomUUID());
            delivery.setWebhookId(webhook.getId());
            delivery.setEventId(event.getEventId());
            delivery.setEventType("EXPIRED");
            delivery.setTargetUrl(webhook.getTargetUrl());
            
            try {
                WebhookEnvelope envelope = new WebhookEnvelope(event.getEventId(), "EXPIRED", null);
                delivery.setPayload(objectMapper.writeValueAsString(envelope));
            } catch (Exception e) {
                logger.error("Failed to serialize webhook envelope", e);
                continue;
            }

            delivery.setStatus("ATTEMPTING");
            delivery.setAttemptCount(1);
            delivery.setCreatedAt(ZonedDateTime.now());
            delivery.setLastAttemptAt(ZonedDateTime.now());
            deliveryRepository.save(delivery);

            // Delegated to WebhookDispatcherService to ensure Spring @Retry AOP proxy intercepts
            dispatcherService.deliverWithRetry(delivery);
        }
    }
}
