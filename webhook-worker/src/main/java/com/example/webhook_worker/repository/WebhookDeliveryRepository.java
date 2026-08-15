package com.example.webhook_worker.repository;

import com.example.webhook_worker.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    List<WebhookDelivery> findByStatusInAndLastAttemptAtBefore(List<String> statuses, ZonedDateTime cutoff);
}
