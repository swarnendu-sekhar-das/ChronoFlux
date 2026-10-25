package com.example.edge_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
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

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "active_from")
    private Instant activeFrom;

    @Column(name = "active_until")
    private Instant activeUntil;

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getActiveFrom() { return activeFrom; }
    public Instant getActiveUntil() { return activeUntil; }
}
