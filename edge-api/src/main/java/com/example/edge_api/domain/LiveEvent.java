package com.example.edge_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "live_events")
public class LiveEvent {

    @Id
    private UUID id;
    
    private String title;
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @jakarta.persistence.Column(name = "created_at")
    private java.time.Instant createdAt;

    @jakarta.persistence.Column(name = "active_from")
    private java.time.Instant activeFrom;

    @jakarta.persistence.Column(name = "active_until")
    private java.time.Instant activeUntil;

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getPayload() { return payload; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public java.time.Instant getActiveFrom() { return activeFrom; }
    public java.time.Instant getActiveUntil() { return activeUntil; }
}
