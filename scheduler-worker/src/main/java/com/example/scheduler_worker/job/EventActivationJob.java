package com.example.scheduler_worker.job;

import com.example.scheduler_worker.service.EventActivationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventActivationJob {

    private static final Logger logger = LoggerFactory.getLogger(EventActivationJob.class);
    private final EventActivationService activationService;

    public EventActivationJob(EventActivationService activationService) {
        this.activationService = activationService;
    }

    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    public void activateScheduledEvents() {
        logger.info("Executing EventActivationJob.");
        activationService.activateScheduledEvents();
    }
}
