package com.example.webhook_worker.service;

import com.example.webhook_worker.domain.WebhookDelivery;
import com.example.webhook_worker.exception.WebhookDeliveryException;
import com.example.webhook_worker.repository.WebhookDeliveryRepository;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;

@Service
public class WebhookDispatcherService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDispatcherService.class);

    private final WebhookHttpClient httpClient;
    private final WebhookDeliveryRepository deliveryRepository;

    public WebhookDispatcherService(WebhookHttpClient httpClient, WebhookDeliveryRepository deliveryRepository) {
        this.httpClient = httpClient;
        this.deliveryRepository = deliveryRepository;
    }

    /**
     * Executes the HTTP dispatch wrapped in Resilience4j's @Retry.
     * Keeping this in an independent Spring Service ensures that invocations pass
     * through Spring's CGLIB dynamic proxy (fixing the internal this-call bypass).
     */
    @Retry(name = "webhookRetry", fallbackMethod = "markAsFailed")
    public void deliverWithRetry(WebhookDelivery delivery) {
        logger.info("Dispatching webhook delivery {} to {}", delivery.getId(), delivery.getTargetUrl());

        int statusCode = httpClient.sendPayload(
            delivery.getTargetUrl(), 
            delivery.getPayload(), 
            delivery.getId().toString()
        );

        // 2xx response received
        delivery.setStatus("SUCCESS");
        delivery.setResponseCode(statusCode);
        delivery.setFailureReason(null);
        delivery.setLastAttemptAt(ZonedDateTime.now());
        deliveryRepository.save(delivery);
        logger.info("Delivery {} successfully delivered with status code {}", delivery.getId(), statusCode);
    }

    /**
     * Resilience4j fallback method.
     * Triggered when retry attempts are exhausted, or immediately on non-retryable 4xx client errors.
     */
    public void markAsFailed(WebhookDelivery delivery, Exception e) {
        logger.warn("Webhook delivery {} marked as failed. Reason: {}", delivery.getId(), e.getMessage());

        if (e instanceof WebhookDeliveryException wde) {
            delivery.setResponseCode(wde.getStatusCode());
            if (!wde.isRetryable()) {
                // 4xx errors should not be retried by the reconciliation job
                delivery.setStatus("PERMANENTLY_FAILED");
            } else {
                delivery.setStatus("FAILED");
            }
        } else {
            delivery.setStatus("FAILED");
            delivery.setResponseCode(500);
        }

        delivery.setFailureReason(e.getMessage());
        delivery.setLastAttemptAt(ZonedDateTime.now());
        deliveryRepository.save(delivery);
    }
}

