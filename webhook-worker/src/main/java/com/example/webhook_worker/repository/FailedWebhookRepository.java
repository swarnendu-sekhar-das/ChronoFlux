package com.example.webhook_worker.repository;

import com.example.webhook_worker.entity.FailedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FailedWebhookRepository extends JpaRepository<FailedWebhook, UUID> {
}
