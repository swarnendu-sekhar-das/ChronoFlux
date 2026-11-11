package com.example.scheduler_worker.job;

import com.example.scheduler_worker.domain.OutboxEvent;
import com.example.scheduler_worker.dto.LiveEventActivatedEvent;
import com.example.scheduler_worker.dto.LiveEventExpiredEvent;
import com.example.scheduler_worker.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class OutboxPublisherJob {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisherJob.class);
    private static final int MAX_OUTBOX_RETRIES = 5;

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisherJob(OutboxEventRepository outboxRepository,
                              RabbitTemplate rabbitTemplate,
                              ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Hybrid Outbox Pattern - Step 2 (Background Poller Safety Net):
     * Runs every 30 seconds to pick up any outbox records that failed the immediate publish
     * (e.g., during RabbitMQ reboot, network partitions, or scheduler JVM crash before dispatch).
     */
    @Scheduled(fixedDelayString = "${scheduler.outbox-polling-rate-ms:30000}")
    @Transactional
    public void publishPendingOutboxEvents() {
        // 10-second cutoff gives immediate AFTER_COMMIT listener time to complete on happy path
        ZonedDateTime cutoff = ZonedDateTime.now().minusSeconds(10);
        List<OutboxEvent> pending = outboxRepository.findPendingOutboxEvents(cutoff, 50);

        if (pending.isEmpty()) {
            return;
        }

        logger.info("Outbox poller found {} un-dispatched events. Executing recovery publishing...", pending.size());

        for (OutboxEvent outbox : pending) {
            if (outbox.getRetryCount() >= MAX_OUTBOX_RETRIES) {
                logger.error("Outbox event {} reached max retries. Marking FAILED.", outbox.getId());
                outbox.setStatus("FAILED");
                outboxRepository.save(outbox);
                continue;
            }

            try {
                Object payloadObj;
                if ("ACTIVATED".equalsIgnoreCase(outbox.getEventType())) {
                    payloadObj = objectMapper.readValue(outbox.getPayload(), LiveEventActivatedEvent.class);
                } else {
                    payloadObj = objectMapper.readValue(outbox.getPayload(), LiveEventExpiredEvent.class);
                }

                rabbitTemplate.convertAndSend("live-ops.events", outbox.getRoutingKey(), payloadObj);
                outbox.setStatus("PUBLISHED");
                outbox.setPublishedAt(ZonedDateTime.now());
                outboxRepository.save(outbox);
                logger.info("Outbox poller successfully published recovered event {} to RabbitMQ", outbox.getId());
            } catch (Exception e) {
                outbox.setRetryCount(outbox.getRetryCount() + 1);
                outboxRepository.save(outbox);
                logger.warn("Outbox poller failed to publish event {} (Attempt {}): {}",
                        outbox.getId(), outbox.getRetryCount(), e.getMessage());
            }
        }
    }
}

