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
    
    private String title;
    private String status;
    private ZonedDateTime activeFrom;

    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public ZonedDateTime getActiveFrom() { return activeFrom; }
    public String getPayload() { return payload; }
}
