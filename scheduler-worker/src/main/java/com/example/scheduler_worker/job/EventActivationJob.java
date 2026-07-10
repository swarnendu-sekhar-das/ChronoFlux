package com.example.scheduler_worker.job;

import com.example.scheduler_worker.domain.LiveEvent;
import com.example.scheduler_worker.repository.LiveEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class EventActivationJob {

    private static final Logger logger = LoggerFactory.getLogger(EventActivationJob.class);

    private final LiveEventRepository repository;
    private final RabbitTemplate rabbitTemplate;

    public EventActivationJob(LiveEventRepository repository, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedRate = 10000) // Runs every 10 seconds
    @Transactional
    public void activateScheduledEvents() {
        ZonedDateTime now = ZonedDateTime.now();
        List<LiveEvent> eventsToActivate = repository.findEventsToActivate("SCHEDULED", now);

        if (eventsToActivate.isEmpty()) {
            logger.debug("No events to activate at {}", now);
            return;
        }

        for (LiveEvent event : eventsToActivate) {
            logger.info("Activating event: {} (ID: {})", event.getTitle(), event.getId());
            
            // 1. Update status in database
            event.setStatus("ACTIVE");
            repository.save(event);

            // 2. Publish message to RabbitMQ for webhook processing
            rabbitTemplate.convertAndSend("events.exchange", "event.active", event.getPayload());
            
            // Note: In a full distributed system, we would also clear the Redis cache here 
            // by publishing a cache invalidation message or directly connecting to Redis.
        }
    }
}
