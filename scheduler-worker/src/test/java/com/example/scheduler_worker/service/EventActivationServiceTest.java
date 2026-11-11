package com.example.scheduler_worker.service;

import com.example.scheduler_worker.domain.LiveEvent;
import com.example.scheduler_worker.domain.OutboxEvent;
import com.example.scheduler_worker.event.EventActivatedSpringEvent;
import com.example.scheduler_worker.repository.LiveEventRepository;
import com.example.scheduler_worker.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventActivationServiceTest {

    @Mock
    private LiveEventRepository repository;

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectProvider<EventActivationService> selfProvider;

    private EventActivationService service;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new EventActivationService(
                repository,
                outboxRepository,
                eventPublisher,
                redisTemplate,
                objectMapper,
                selfProvider
        );
    }

    @Test
    void activateNextBatch_SavesActiveState_PersistsOutboxAndPublishesSpringEvent() {
        UUID eventId = UUID.randomUUID();
        UUID schemaId = UUID.randomUUID();

        LiveEvent mockEvent = mock(LiveEvent.class);
        when(mockEvent.getId()).thenReturn(eventId);
        when(mockEvent.getTitle()).thenReturn("Seasonal Quest");
        when(mockEvent.getSchemaId()).thenReturn(schemaId);
        when(mockEvent.getPayload()).thenReturn("{\"quest\":\"kill_dragon\"}");

        when(repository.findEventsToActivate(eq("SCHEDULED"), anyInt())).thenReturn(List.of(mockEvent));

        int activatedCount = service.activateNextBatch();

        assertEquals(1, activatedCount);
        verify(mockEvent, times(1)).setStatus("ACTIVE");
        verify(repository, times(1)).save(mockEvent);

        // Verify Transactional Outbox write
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository, times(1)).save(outboxCaptor.capture());
        OutboxEvent savedOutbox = outboxCaptor.getValue();
        assertEquals("PENDING", savedOutbox.getStatus());
        assertEquals("ACTIVATED", savedOutbox.getEventType());
        assertEquals("event.active", savedOutbox.getRoutingKey());
        assertEquals(eventId, savedOutbox.getEventId());

        // Verify Spring Event published with outbox ID
        ArgumentCaptor<EventActivatedSpringEvent> eventCaptor = ArgumentCaptor.forClass(EventActivatedSpringEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue().getOutboxId());
        assertEquals(savedOutbox.getId(), eventCaptor.getValue().getOutboxId());
    }
}
