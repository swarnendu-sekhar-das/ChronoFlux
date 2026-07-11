package com.example.scheduler_worker.job;

import com.example.scheduler_worker.service.EventActivationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventActivationJob {

    private final EventActivationService activationService;

    public EventActivationJob(EventActivationService activationService) {
        this.activationService = activationService;
    }

    @Scheduled(fixedRate = 10000) // Runs every 10 seconds
    public void activateScheduledEvents() {
        activationService.activateScheduledEvents();
    }
}
