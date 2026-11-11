package com.example.scheduler_worker.event;

import com.example.scheduler_worker.dto.LiveEventExpiredEvent;
import java.util.UUID;

public class EventExpiredSpringEvent {
    private final LiveEventExpiredEvent payload;
    private final UUID outboxId;

    public EventExpiredSpringEvent(LiveEventExpiredEvent payload, UUID outboxId) {
        this.payload = payload;
        this.outboxId = outboxId;
    }

    public LiveEventExpiredEvent getPayload() {
        return payload;
    }

    public UUID getOutboxId() {
        return outboxId;
    }
}
