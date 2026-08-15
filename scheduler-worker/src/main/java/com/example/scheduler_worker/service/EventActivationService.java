package com.example.scheduler_worker.service;

import com.example.scheduler_worker.domain.LiveEvent;
import com.example.scheduler_worker.dto.LiveEventActivatedEvent;
import com.example.scheduler_worker.repository.LiveEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventActivationService {

    private static final Logger logger = LoggerFactory.getLogger(EventActivationService.class);

    private final LiveEventRepository repository;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${scheduler.batch-size:50}")
    private int batchSize;

    private final org.springframework.beans.factory.ObjectProvider<EventActivationService> selfProvider;

    public EventActivationService(LiveEventRepository repository, 
                                  org.springframework.context.ApplicationEventPublisher applicationEventPublisher, 
                                  StringRedisTemplate redisTemplate, 
                                  ObjectMapper objectMapper,
                                  org.springframework.beans.factory.ObjectProvider<EventActivationService> selfProvider) {
        this.repository = repository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.selfProvider = selfProvider;
    }

    public void activateScheduledEvents() {
        ZonedDateTime now = ZonedDateTime.now();
        EventActivationService self = selfProvider.getIfAvailable();
        if (self == null) self = this;

        while (self.expireNextBatch(now) == batchSize) {
            logger.debug("Expired a full batch of {} events, checking for more...", batchSize);
        }
        while (self.activateNextBatch(now) == batchSize) {
            logger.debug("Activated a full batch of {} events, checking for more...", batchSize);
        }
    }

    @Transactional
    public int expireNextBatch(ZonedDateTime now) {
        List<LiveEvent> eventsToExpire = repository.findEventsToExpire("ACTIVE", now, batchSize);
        for (LiveEvent event : eventsToExpire) {
            logger.info("Expiring event: {} (ID: {})", event.getTitle(), event.getId());
            event.setStatus("EXPIRED");
            repository.save(event);

            // Granular Cache Removal
            redisTemplate.opsForSet().remove("active_event_ids", event.getId().toString());
            redisTemplate.delete("events::" + event.getId());
        }
        return eventsToExpire.size();
    }

    @Transactional
    public int activateNextBatch(ZonedDateTime now) {
        List<LiveEvent> eventsToActivate = repository.findEventsToActivate("SCHEDULED", now, batchSize);
        for (LiveEvent event : eventsToActivate) {
            logger.info("Activating event: {} (ID: {})", event.getTitle(), event.getId());
            event.setStatus("ACTIVE");
            repository.save(event);
            
            // Granular Cache Addition
            try {
                Map<String, Object> dto = toDTO(event);
                String json = objectMapper.writeValueAsString(dto);
                redisTemplate.opsForSet().add("active_event_ids", event.getId().toString());
                redisTemplate.opsForValue().set("events::" + event.getId(), json);
            } catch (Exception e) {
                logger.warn("Failed to update Redis cache for event {}", event.getId(), e);
            }

            LiveEventActivatedEvent payload = new LiveEventActivatedEvent(
                    event.getId(),
                    event.getSchemaId(),
                    event.getPayload()
            );
            // Using @TransactionalEventListener to fire RabbitMQ only after DB commit
            applicationEventPublisher.publishEvent(new com.example.scheduler_worker.event.EventActivatedSpringEvent(payload));
        }
        return eventsToActivate.size();
    }

    private Map<String, Object> toDTO(LiveEvent event) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", event.getId().toString());
        dto.put("title", event.getTitle());
        dto.put("status", event.getStatus());
        dto.put("payload", event.getPayload());
        dto.put("activeFrom", event.getActiveFrom() != null ? event.getActiveFrom().toInstant().toString() : null);
        dto.put("activeUntil", event.getActiveUntil() != null ? event.getActiveUntil().toInstant().toString() : null);
        // We omit createdAt to match the Edge API DTO closely, or we can add it if needed
        return dto;
    }
}
