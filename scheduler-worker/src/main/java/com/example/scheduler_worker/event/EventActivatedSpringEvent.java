package com.example.scheduler_worker.event;

import com.example.scheduler_worker.dto.LiveEventActivatedEvent;

public class EventActivatedSpringEvent {
    private final LiveEventActivatedEvent payload;

    public EventActivatedSpringEvent(LiveEventActivatedEvent payload) {
        this.payload = payload;
    }

    public LiveEventActivatedEvent getPayload() {
        return payload;
    }
}
