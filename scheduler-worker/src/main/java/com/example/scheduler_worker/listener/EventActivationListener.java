package com.example.scheduler_worker.listener;

import com.example.scheduler_worker.event.EventActivatedSpringEvent;
import com.example.scheduler_worker.event.EventExpiredSpringEvent;
import com.example.scheduler_worker.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZonedDateTime;

@Component
public class EventActivationListener {

    private static final Logger logger = LoggerFactory.getLogger(EventActivationListener.class);
    private final RabbitTemplate rabbitTemplate;
    private final OutboxEventRepository outboxRepository;

    public EventActivationListener(RabbitTemplate rabbitTemplate, OutboxEventRepository outboxRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Hybrid Outbox Pattern - Step 1 (Immediate Publish on Happy Path):
     * Executes immediately AFTER_COMMIT for sub-millisecond dispatch.
     * If RabbitMQ is reachable, outbox record is marked PUBLISHED.
     * If RabbitMQ is down, the outbox record remains PENDING for the safety-net poller.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEventActivation(EventActivatedSpringEvent event) {
        logger.info("Transaction committed. Attempting immediate RabbitMQ publish for Event ID: {}", event.getPayload().getEventId());
        try {
            rabbitTemplate.convertAndSend("live-ops.events", "event.active", event.getPayload());
            if (event.getOutboxId() != null) {
                outboxRepository.findById(event.getOutboxId()).ifPresent(outbox -> {
                    outbox.setStatus("PUBLISHED");
                    outbox.setPublishedAt(ZonedDateTime.now());
                    outboxRepository.save(outbox);
                });
            }
            logger.info("Successfully published active event to RabbitMQ (Outbox ID: {})", event.getOutboxId());
        } catch (Exception e) {
            logger.warn("Immediate RabbitMQ publish failed for event {} (Outbox ID: {}). Will be recovered by OutboxPublisherJob: {}",
                    event.getPayload().getEventId(), event.getOutboxId(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEventExpiration(EventExpiredSpringEvent event) {
        logger.info("Transaction committed. Attempting immediate RabbitMQ expiration publish for Event ID: {}", event.getPayload().getEventId());
        try {
            rabbitTemplate.convertAndSend("live-ops.events", "event.expired", event.getPayload());
            if (event.getOutboxId() != null) {
                outboxRepository.findById(event.getOutboxId()).ifPresent(outbox -> {
                    outbox.setStatus("PUBLISHED");
                    outbox.setPublishedAt(ZonedDateTime.now());
                    outboxRepository.save(outbox);
                });
            }
            logger.info("Successfully published expired event to RabbitMQ (Outbox ID: {})", event.getOutboxId());
        } catch (Exception e) {
            logger.warn("Immediate RabbitMQ expiration publish failed for event {} (Outbox ID: {}). Will be recovered by OutboxPublisherJob: {}",
                    event.getPayload().getEventId(), event.getOutboxId(), e.getMessage());
        }
    }
}
