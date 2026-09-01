package com.example.scheduler_worker.event;

import com.example.scheduler_worker.dto.LiveEventExpiredEvent;

public class EventExpiredSpringEvent {
    private final LiveEventExpiredEvent payload;

    public EventExpiredSpringEvent(LiveEventExpiredEvent payload) {
        this.payload = payload;
    }

    public LiveEventExpiredEvent getPayload() {
        return payload;
    }
}
