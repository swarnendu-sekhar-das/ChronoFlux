package com.example.scheduler_worker.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_events")
public class LiveEvent {

    @Id
    private UUID id;
    
    private UUID schemaId;
    private String title;
    private String status;
    private ZonedDateTime activeFrom;
    private ZonedDateTime activeUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    public UUID getId() { return id; }
    public UUID getSchemaId() { return schemaId; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public ZonedDateTime getActiveFrom() { return activeFrom; }
    public ZonedDateTime getActiveUntil() { return activeUntil; }
    public String getPayload() { return payload; }
}
