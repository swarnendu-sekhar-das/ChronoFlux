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
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    public EventActivationService(LiveEventRepository repository, RabbitTemplate rabbitTemplate, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void activateScheduledEvents() {
        ZonedDateTime now = ZonedDateTime.now();
        boolean cacheNeedsUpdate = false;

        // 1. Expire Events
        List<LiveEvent> eventsToExpire = repository.findEventsToExpire("ACTIVE", now);
        for (LiveEvent event : eventsToExpire) {
            logger.info("Expiring event: {} (ID: {})", event.getTitle(), event.getId());
            event.setStatus("EXPIRED");
            repository.save(event);
            cacheNeedsUpdate = true;
        }

        // 2. Activate Events
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
            rabbitTemplate.convertAndSend("live-ops.events", "event.active", payload);
            cacheNeedsUpdate = true;
        }

        // 3. Invalidate / Warm Cache
        if (cacheNeedsUpdate) {
            logger.info("Invalidating Redis cache 'activeEvents::all'");
            redisTemplate.delete("activeEvents::all");
        }
    }
}
