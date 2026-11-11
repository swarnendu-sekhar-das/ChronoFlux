package com.chronoflux.adminapi.dto;

import java.time.Instant;
import java.util.UUID;

public class WebhookDeliveryDTO {
    private UUID id;
    private UUID webhookId;
    private UUID eventId;
    private String eventType;
    private String status;
    private Integer responseCode;
    private Integer attemptCount;
    private Instant lastAttemptAt;
    private Instant createdAt;
    private String targetUrl;
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
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
