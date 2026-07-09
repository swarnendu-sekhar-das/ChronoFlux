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

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getPayload() { return payload; }
}
