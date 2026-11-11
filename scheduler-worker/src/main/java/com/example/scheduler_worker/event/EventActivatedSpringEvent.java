package com.example.scheduler_worker.event;

import com.example.scheduler_worker.dto.LiveEventActivatedEvent;
import java.util.UUID;

public class EventActivatedSpringEvent {
    private final LiveEventActivatedEvent payload;
    private final UUID outboxId;

    public EventActivatedSpringEvent(LiveEventActivatedEvent payload, UUID outboxId) {
        this.payload = payload;
        this.outboxId = outboxId;
    }

    public LiveEventActivatedEvent getPayload() {
        return payload;
    }

    public UUID getOutboxId() {
        return outboxId;
    }
}
