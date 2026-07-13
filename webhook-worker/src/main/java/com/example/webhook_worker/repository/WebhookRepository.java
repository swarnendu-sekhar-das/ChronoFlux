package com.example.webhook_worker.repository;

import com.example.webhook_worker.domain.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
    List<Webhook> findBySchemaIdAndIsActiveTrue(UUID schemaId);
}
