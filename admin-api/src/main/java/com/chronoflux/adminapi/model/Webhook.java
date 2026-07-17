package com.chronoflux.adminapi.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhooks")
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "schema_id")
    private UUID schemaId;

    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSchemaId() { return schemaId; }
    public void setSchemaId(UUID schemaId) { this.schemaId = schemaId; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
