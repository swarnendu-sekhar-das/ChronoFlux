package com.example.webhook_worker.job;

import com.example.webhook_worker.domain.WebhookDelivery;
import com.example.webhook_worker.exception.WebhookDeliveryException;
import com.example.webhook_worker.repository.WebhookDeliveryRepository;
import com.example.webhook_worker.service.WebhookHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class DeliveryReconciliationJob {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryReconciliationJob.class);
    private static final int MAX_RETRIES = 5;

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookHttpClient httpClient;
    private final TransactionTemplate transactionTemplate;

    public DeliveryReconciliationJob(WebhookDeliveryRepository deliveryRepository,
                                     WebhookHttpClient httpClient, 
                                     TransactionTemplate transactionTemplate) {
        this.deliveryRepository = deliveryRepository;
        this.httpClient = httpClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    public void reconcileZombieDeliveries() {
        // Cutoff window of 5 minutes: ensures we don't interfere with deliveries currently executing
        ZonedDateTime cutoff = ZonedDateTime.now().minusMinutes(5);
        
        List<WebhookDelivery> zombies = transactionTemplate.execute(status -> {
            // Include RETRYING so that deliveries whose worker crashed mid-batch are not lost forever
            List<WebhookDelivery> found = deliveryRepository
                .findZombiesForReconciliation(List.of("ATTEMPTING", "FAILED", "RETRYING"), cutoff);
            for (WebhookDelivery delivery : found) {
                delivery.setStatus("RETRYING");
                delivery.setLastAttemptAt(ZonedDateTime.now());
            }
            if (!found.isEmpty()) {
                deliveryRepository.saveAll(found);
            }
            return found;
        });

        if (zombies == null || zombies.isEmpty()) {
            return;
        }

        logger.info("Reconciliation sweep found {} zombie/retryable deliveries.", zombies.size());

        for (WebhookDelivery delivery : zombies) {
            if (delivery.getAttemptCount() >= MAX_RETRIES) {
                delivery.setStatus("PERMANENTLY_FAILED");
                delivery.setLastAttemptAt(ZonedDateTime.now());
                deliveryRepository.save(delivery);
                logger.warn("Delivery {} permanently failed after {} attempts.", delivery.getId(), MAX_RETRIES);
                continue;
            }

            try {
                int statusCode = httpClient.sendPayload(
                    delivery.getTargetUrl(), 
                    delivery.getPayload(), 
                    delivery.getId().toString()
                );
                delivery.setStatus("SUCCESS");
                delivery.setResponseCode(statusCode);
                delivery.setFailureReason(null);
            } catch (WebhookDeliveryException e) {
                delivery.setResponseCode(e.getStatusCode());
                if (!e.isRetryable()) {
                    // 4xx client errors are non-retryable; fail immediately
                    delivery.setStatus("PERMANENTLY_FAILED");
                } else {
                    delivery.setStatus("FAILED");
                }
                delivery.setFailureReason(e.getMessage());
            } catch (Exception e) {
                delivery.setStatus("FAILED");
                delivery.setResponseCode(500);
                delivery.setFailureReason(e.getMessage());
            }

            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            delivery.setLastAttemptAt(ZonedDateTime.now());
            deliveryRepository.save(delivery);
        }
    }
}
