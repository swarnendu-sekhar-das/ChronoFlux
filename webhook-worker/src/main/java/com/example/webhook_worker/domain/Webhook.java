package com.example.webhook_worker.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhooks")
public class Webhook {
    @Id
    private UUID id;
    private UUID schemaId;
    private String targetUrl;
    private Boolean isActive;
    private ZonedDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSchemaId() { return schemaId; }
    public void setSchemaId(UUID schemaId) { this.schemaId = schemaId; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
