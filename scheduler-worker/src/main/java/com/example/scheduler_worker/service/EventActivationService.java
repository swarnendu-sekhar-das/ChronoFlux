package com.example.scheduler_worker.service;

import com.example.scheduler_worker.domain.LiveEvent;
import com.example.scheduler_worker.domain.OutboxEvent;
import com.example.scheduler_worker.dto.LiveEventActivatedEvent;
import com.example.scheduler_worker.dto.LiveEventExpiredEvent;
import com.example.scheduler_worker.event.EventActivatedSpringEvent;
import com.example.scheduler_worker.event.EventExpiredSpringEvent;
import com.example.scheduler_worker.repository.LiveEventRepository;
import com.example.scheduler_worker.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventActivationService {

    private static final Logger logger = LoggerFactory.getLogger(EventActivationService.class);

    private final LiveEventRepository repository;
    private final OutboxEventRepository outboxRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${scheduler.batch-size:50}")
    private int batchSize;

    private final ObjectProvider<EventActivationService> selfProvider;

    public EventActivationService(LiveEventRepository repository, 
                                  OutboxEventRepository outboxRepository,
                                  ApplicationEventPublisher applicationEventPublisher, 
                                  StringRedisTemplate redisTemplate, 
                                  ObjectMapper objectMapper,
                                  ObjectProvider<EventActivationService> selfProvider) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.selfProvider = selfProvider;
    }

    public void activateScheduledEvents() {
        EventActivationService self = selfProvider.getIfAvailable();
        if (self == null) self = this;

        while (self.expireNextBatch() == batchSize) {
            logger.debug("Expired a full batch of {} events, checking for more...", batchSize);
        }
        while (self.activateNextBatch() == batchSize) {
            logger.debug("Activated a full batch of {} events, checking for more...", batchSize);
        }
    }

    @Transactional
    public int expireNextBatch() {
        List<LiveEvent> eventsToExpire = repository.findEventsToExpire("ACTIVE", batchSize);
        for (LiveEvent event : eventsToExpire) {
            logger.info("Expiring event: {} (ID: {})", event.getTitle(), event.getId());
            event.setStatus("EXPIRED");
            repository.save(event);

            // 1. Transactional Outbox Pattern: Persist outbox event inside same ACID transaction
            OutboxEvent outbox = new OutboxEvent();
            outbox.setId(UUID.randomUUID());
            outbox.setEventId(event.getId());
            outbox.setEventType("EXPIRED");
            outbox.setRoutingKey("event.expired");
            LiveEventExpiredEvent payload = new LiveEventExpiredEvent(event.getId(), event.getSchemaId());
            try {
                outbox.setPayload(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                logger.error("Failed to serialize outbox payload for expired event {}", event.getId(), e);
                continue;
            }
            outbox.setStatus("PENDING");
            outbox.setCreatedAt(ZonedDateTime.now());
            outboxRepository.save(outbox);

            // 2. Defer Redis cache eviction to AFTER_COMMIT so cache is not evicted if DB commit fails
            UUID eventId = event.getId();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            redisTemplate.opsForSet().remove("active_event_ids", eventId.toString());
                            redisTemplate.delete("events::" + eventId);
                        } catch (Exception e) {
                            logger.warn("Failed to evict Redis cache after commit for event {}", eventId, e);
                        }
                    }
                });
            } else {
                try {
                    redisTemplate.opsForSet().remove("active_event_ids", eventId.toString());
                    redisTemplate.delete("events::" + eventId);
                } catch (Exception e) {
                    logger.warn("Failed to evict Redis cache for event {}", eventId, e);
                }
            }

            // 3. Emit Spring event for immediate AFTER_COMMIT RabbitMQ publish attempt
            applicationEventPublisher.publishEvent(new EventExpiredSpringEvent(payload, outbox.getId()));
        }
        return eventsToExpire.size();
    }

    @Transactional
    public int activateNextBatch() {
        List<LiveEvent> eventsToActivate = repository.findEventsToActivate("SCHEDULED", batchSize);
        for (LiveEvent event : eventsToActivate) {
            logger.info("Activating event: {} (ID: {})", event.getTitle(), event.getId());
            event.setStatus("ACTIVE");
            repository.save(event);

            // 1. Transactional Outbox Pattern: Persist outbox event inside same ACID transaction
            OutboxEvent outbox = new OutboxEvent();
            outbox.setId(UUID.randomUUID());
            outbox.setEventId(event.getId());
            outbox.setEventType("ACTIVATED");
            outbox.setRoutingKey("event.active");
            LiveEventActivatedEvent payload = new LiveEventActivatedEvent(
                    event.getId(),
                    event.getSchemaId(),
                    event.getPayload()
            );
            try {
                outbox.setPayload(objectMapper.writeValueAsString(payload));
            } catch (Exception e) {
                logger.error("Failed to serialize outbox payload for event {}", event.getId(), e);
                continue;
            }
            outbox.setStatus("PENDING");
            outbox.setCreatedAt(ZonedDateTime.now());
            outboxRepository.save(outbox);

            // 2. Defer Redis cache warming to AFTER_COMMIT to prevent phantom active events on DB rollback
            UUID eventId = event.getId();
            Map<String, Object> dto = toDTO(event);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            String json = objectMapper.writeValueAsString(dto);
                            redisTemplate.opsForSet().add("active_event_ids", eventId.toString());
                            redisTemplate.opsForValue().set("events::" + eventId, json);
                        } catch (Exception e) {
                            logger.warn("Failed to update Redis cache after commit for event {}", eventId, e);
                        }
                    }
                });
            } else {
                try {
                    String json = objectMapper.writeValueAsString(dto);
                    redisTemplate.opsForSet().add("active_event_ids", eventId.toString());
                    redisTemplate.opsForValue().set("events::" + eventId, json);
                } catch (Exception e) {
                    logger.warn("Failed to update Redis cache for event {}", eventId, e);
                }
            }

            // 3. Emit Spring event for immediate AFTER_COMMIT RabbitMQ publish attempt
            applicationEventPublisher.publishEvent(new EventActivatedSpringEvent(payload, outbox.getId()));
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
        return dto;
    }
}
