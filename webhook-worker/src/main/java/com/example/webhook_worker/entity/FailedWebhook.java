package com.example.webhook_worker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "failed_webhooks")
public class FailedWebhook {

    @Id
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(name = "failed_at")
    private ZonedDateTime failedAt;

    @Column(columnDefinition = "text")
    private String errorReason;

    public FailedWebhook() {
    }

    public FailedWebhook(UUID eventId, String payload, String errorReason) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.payload = payload;
        this.errorReason = errorReason;
        this.failedAt = ZonedDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public ZonedDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(ZonedDateTime failedAt) { this.failedAt = failedAt; }
    public String getErrorReason() { return errorReason; }
    public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
}
