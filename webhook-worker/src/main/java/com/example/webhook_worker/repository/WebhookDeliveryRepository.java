package com.example.webhook_worker.repository;

import com.example.webhook_worker.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM webhook_deliveries WHERE status IN :statuses AND last_attempt_at < :cutoff FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<WebhookDelivery> findZombiesForReconciliation(@org.springframework.data.repository.query.Param("statuses") List<String> statuses, @org.springframework.data.repository.query.Param("cutoff") ZonedDateTime cutoff);
}
