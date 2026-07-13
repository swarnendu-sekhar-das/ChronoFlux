package com.example.webhook_worker.repository;

import com.example.webhook_worker.domain.WebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
}
