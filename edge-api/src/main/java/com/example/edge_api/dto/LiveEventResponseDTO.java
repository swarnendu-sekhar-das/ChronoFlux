package com.example.edge_api.dto;

import java.util.UUID;

public class LiveEventResponseDTO {
    private UUID id;
    private String title;
    private String status;
    private String payload;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}
