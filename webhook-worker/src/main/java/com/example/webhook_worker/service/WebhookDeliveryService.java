package com.example.webhook_worker.service;

import com.example.webhook_worker.domain.Webhook;
import com.example.webhook_worker.domain.WebhookDelivery;
import com.example.webhook_worker.dto.LiveEventActivatedEvent;
import com.example.webhook_worker.repository.WebhookDeliveryRepository;
import com.example.webhook_worker.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDeliveryService.class);

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final com.example.webhook_worker.repository.FailedWebhookRepository failedWebhookRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public WebhookDeliveryService(WebhookRepository webhookRepository, WebhookDeliveryRepository deliveryRepository, com.example.webhook_worker.repository.FailedWebhookRepository failedWebhookRepository, WebClient webClient, ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
        this.failedWebhookRepository = failedWebhookRepository;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
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
                deliverWebhook(delivery, event.getPayload(), webhook.getTargetUrl());
            } catch (Exception e) {
                logger.error("Webhook delivery failed for delivery ID: {}", delivery.getId());
                failedWebhookRepository.save(new com.example.webhook_worker.entity.FailedWebhook(event.getEventId(), event.getPayload(), e.getMessage()));
            }
        }
    }

    public void deliverWebhook(WebhookDelivery delivery, String payload, String targetUrl) throws Exception {
        delivery.setAttemptCount(1);
        delivery.setLastAttemptAt(ZonedDateTime.now());
        delivery.setStatus("ATTEMPTING");
        deliveryRepository.save(delivery);
        
        logger.info("Attempt {} delivering to URL: {}", delivery.getAttemptCount(), targetUrl);

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);

            webClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonNode)
                    .retrieve()
                    .toBodilessEntity()
                    .block(); // Block since we are in a message listener thread
            
            delivery.setStatus("SUCCESS");
            delivery.setResponseCode(200);
            deliveryRepository.save(delivery);
            logger.info("Delivery {} successful", delivery.getId());
        } catch (Exception e) {
            logger.error("Delivery {} failed: {}", delivery.getId(), e.getMessage());
            delivery.setStatus("FAILED");
            deliveryRepository.save(delivery);
            throw e;
        }
    }
}
