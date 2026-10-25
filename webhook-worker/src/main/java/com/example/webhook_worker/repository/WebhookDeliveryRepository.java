package com.example.webhook_worker.repository;

import com.example.webhook_worker.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    @Query(value = "SELECT * FROM webhook_deliveries WHERE status IN :statuses AND last_attempt_at < :cutoff FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<WebhookDelivery> findZombiesForReconciliation(@Param("statuses") List<String> statuses, @Param("cutoff") ZonedDateTime cutoff);
}
