package com.example.scheduler_worker.dto;

import java.util.UUID;

public class LiveEventActivatedEvent {
    private UUID eventId;
    private UUID schemaId;
    private String payload;

    public LiveEventActivatedEvent() {}

    public LiveEventActivatedEvent(UUID eventId, UUID schemaId, String payload) {
        this.eventId = eventId;
        this.schemaId = schemaId;
        this.payload = payload;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(UUID schemaId) {
        this.schemaId = schemaId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
