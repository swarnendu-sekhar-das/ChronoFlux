package com.example.webhook_worker.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {
    @Id
    private UUID id;

    @Column(name = "webhook_id")
    private UUID webhookId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "status")
    private String status;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "last_attempt_at")
    private ZonedDateTime lastAttemptAt;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "target_url")
    private String targetUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(name = "failure_reason")
    private String failureReason;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getWebhookId() { return webhookId; }
    public void setWebhookId(UUID webhookId) { this.webhookId = webhookId; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public ZonedDateTime getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(ZonedDateTime lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
