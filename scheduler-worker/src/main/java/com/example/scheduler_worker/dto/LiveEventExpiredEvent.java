package com.example.scheduler_worker.dto;

import java.util.UUID;

public class LiveEventExpiredEvent {
    private UUID eventId;
    private UUID schemaId;

    public LiveEventExpiredEvent() {}

    public LiveEventExpiredEvent(UUID eventId, UUID schemaId) {
        this.eventId = eventId;
        this.schemaId = schemaId;
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
}
