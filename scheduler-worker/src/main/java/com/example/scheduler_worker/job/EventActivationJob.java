package com.example.scheduler_worker.job;

import com.example.scheduler_worker.service.EventActivationService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventActivationJob {

    private final EventActivationService activationService;

    public EventActivationJob(EventActivationService activationService) {
        this.activationService = activationService;
    }

    @Scheduled(cron = "0 * * * * *") // Runs at the start of every minute
    @SchedulerLock(name = "activate_events", lockAtMostFor = "50s", lockAtLeastFor = "10s")
    public void activateScheduledEvents() {
        activationService.activateScheduledEvents();
    }
}
