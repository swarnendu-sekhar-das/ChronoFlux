package com.example.edge_api.dto;

import java.util.UUID;

public class LiveEventResponseDTO {
    private UUID id;
    private String title;
    private String status;
    private String payload;
    private String createdAt;
    private String activeFrom;
    private String activeUntil;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getActiveFrom() { return activeFrom; }
    public void setActiveFrom(String activeFrom) { this.activeFrom = activeFrom; }
    public String getActiveUntil() { return activeUntil; }
    public void setActiveUntil(String activeUntil) { this.activeUntil = activeUntil; }
}
