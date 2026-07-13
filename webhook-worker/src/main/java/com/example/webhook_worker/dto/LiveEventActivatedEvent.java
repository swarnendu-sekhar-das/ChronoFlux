package com.example.webhook_worker.dto;

import java.util.UUID;

public class LiveEventActivatedEvent {
    private UUID eventId;
    private UUID schemaId;
    private String payload;

    public LiveEventActivatedEvent() {}

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
