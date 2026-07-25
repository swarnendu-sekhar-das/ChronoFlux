package com.example.scheduler_worker.service;

import com.example.scheduler_worker.domain.LiveEvent;
import com.example.scheduler_worker.dto.LiveEventActivatedEvent;
import com.example.scheduler_worker.repository.LiveEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class EventActivationService {

    private static final Logger logger = LoggerFactory.getLogger(EventActivationService.class);

    private final LiveEventRepository repository;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    private final StringRedisTemplate redisTemplate;

    public EventActivationService(LiveEventRepository repository, org.springframework.context.ApplicationEventPublisher applicationEventPublisher, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void activateScheduledEvents() {
        ZonedDateTime now = ZonedDateTime.now();
        boolean cacheNeedsUpdate = false;

        // Step 1: expire events whose activeUntil has passed
        List<LiveEvent> eventsToExpire = repository.findEventsToExpire("ACTIVE", now);
        for (LiveEvent event : eventsToExpire) {
            logger.info("Expiring event: {} (ID: {})", event.getTitle(), event.getId());
            event.setStatus("EXPIRED");
            repository.save(event);
            cacheNeedsUpdate = true;
        }

        // Step 2: activate events whose activeFrom time has arrived
        List<LiveEvent> eventsToActivate = repository.findEventsToActivate("SCHEDULED", now);
        for (LiveEvent event : eventsToActivate) {
            logger.info("Activating event: {} (ID: {})", event.getTitle(), event.getId());
            event.setStatus("ACTIVE");
            repository.save(event);
            
            LiveEventActivatedEvent payload = new LiveEventActivatedEvent(
                    event.getId(),
                    event.getSchemaId(),
                    event.getPayload()
            );
            // Using @TransactionalEventListener to fire RabbitMQ only after DB commit — avoids dual-write inconsistency
            applicationEventPublisher.publishEvent(new com.example.scheduler_worker.event.EventActivatedSpringEvent(payload));
            cacheNeedsUpdate = true;
        }

        // Step 3: if anything changed, evict all paginated cache entries so edge-api serves fresh data
        if (cacheNeedsUpdate) {
            logger.info("Invalidating Redis cache 'activeEvents*'");
            redisTemplate.delete(redisTemplate.keys("activeEvents*"));
        }
    }
}
