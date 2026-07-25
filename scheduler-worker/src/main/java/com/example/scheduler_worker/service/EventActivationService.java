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

    @org.springframework.beans.factory.annotation.Value("${scheduler.batch-size:50}")
    private int batchSize;

    private final org.springframework.beans.factory.ObjectProvider<EventActivationService> selfProvider;

    public EventActivationService(LiveEventRepository repository, org.springframework.context.ApplicationEventPublisher applicationEventPublisher, StringRedisTemplate redisTemplate, org.springframework.beans.factory.ObjectProvider<EventActivationService> selfProvider) {
        this.repository = repository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.redisTemplate = redisTemplate;
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
        }
        if (!eventsToExpire.isEmpty()) {
            logger.info("Invalidating Redis cache 'activeEvents*'");
            redisTemplate.delete(redisTemplate.keys("activeEvents*"));
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
            
            LiveEventActivatedEvent payload = new LiveEventActivatedEvent(
                    event.getId(),
                    event.getSchemaId(),
                    event.getPayload()
            );
            // Using @TransactionalEventListener to fire RabbitMQ only after DB commit — avoids dual-write inconsistency
            applicationEventPublisher.publishEvent(new com.example.scheduler_worker.event.EventActivatedSpringEvent(payload));
        }
        if (!eventsToActivate.isEmpty()) {
            logger.info("Invalidating Redis cache 'activeEvents*'");
            redisTemplate.delete(redisTemplate.keys("activeEvents*"));
        }
        return eventsToActivate.size();
    }
}
