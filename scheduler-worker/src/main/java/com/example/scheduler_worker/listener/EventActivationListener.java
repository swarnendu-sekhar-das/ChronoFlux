package com.example.scheduler_worker.listener;

import com.example.scheduler_worker.event.EventActivatedSpringEvent;
import com.example.scheduler_worker.event.EventExpiredSpringEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventActivationListener {
    private static final Logger logger = LoggerFactory.getLogger(EventActivationListener.class);
    private final RabbitTemplate rabbitTemplate;

    public EventActivationListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEventActivation(EventActivatedSpringEvent event) {
        logger.info("Transaction committed. Sending event to RabbitMQ for Event ID: {}", event.getPayload().getEventId());
        rabbitTemplate.convertAndSend("live-ops.events", "event.active", event.getPayload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEventExpiration(EventExpiredSpringEvent event) {
        logger.info("Transaction committed. Sending expiration to RabbitMQ for Event ID: {}", event.getPayload().getEventId());
        rabbitTemplate.convertAndSend("live-ops.events", "event.expired", event.getPayload());
    }
}
