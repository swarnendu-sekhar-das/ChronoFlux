package com.example.scheduler_worker.job;

import com.example.scheduler_worker.service.EventActivationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventActivationJob {

    private static final Logger logger = LoggerFactory.getLogger(EventActivationJob.class);
    private final EventActivationService activationService;

    @Value("${scheduler.max-jitter-ms:3000}")
    private long maxJitterMs;

    public EventActivationJob(EventActivationService activationService) {
        this.activationService = activationService;
    }

    @Scheduled(fixedRateString = "${scheduler.polling-rate-ms:60000}") // poll DB every minute to activate/expire events
    public void activateScheduledEvents() throws InterruptedException {
        long jitterMs = (long) (Math.random() * maxJitterMs);
        if (jitterMs > 0) {
            Thread.sleep(jitterMs);
        }
        logger.info("Executing EventActivationJob with jitter {}ms.", jitterMs);
        activationService.activateScheduledEvents();
    }
}
