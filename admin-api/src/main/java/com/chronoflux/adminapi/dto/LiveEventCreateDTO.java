package com.chronoflux.adminapi.dto;

import com.chronoflux.adminapi.model.EventStatus;

import java.time.Instant;
import java.util.Map;

public class LiveEventCreateDTO {
    private EventSchemaIdDTO schema;
    private String title;
    private EventStatus status;
    private Map<String, Object> payload;
    private Instant activeFrom;
    private Instant activeUntil;

    public EventSchemaIdDTO getSchema() { return schema; }
    public void setSchema(EventSchemaIdDTO schema) { this.schema = schema; }
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
}
