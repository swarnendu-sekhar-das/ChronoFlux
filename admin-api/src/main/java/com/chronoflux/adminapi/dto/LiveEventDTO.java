package com.chronoflux.adminapi.dto;

import com.chronoflux.adminapi.model.EventStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class LiveEventDTO {
    private UUID id;
    private EventSchemaDTO schema;
    private String title;
    private EventStatus status;
    private Map<String, Object> payload;
    private Instant activeFrom;
    private Instant activeUntil;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public EventSchemaDTO getSchema() { return schema; }
    public void setSchema(EventSchemaDTO schema) { this.schema = schema; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public Instant getActiveFrom() { return activeFrom; }
    public void setActiveFrom(Instant activeFrom) { this.activeFrom = activeFrom; }
    public Instant getActiveUntil() { return activeUntil; }
    public void setActiveUntil(Instant activeUntil) { this.activeUntil = activeUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
