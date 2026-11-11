package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.dto.LiveEventCreateDTO;
import com.chronoflux.adminapi.dto.LiveEventUpdateDTO;
import com.chronoflux.adminapi.mapper.LiveEventMapper;
import com.chronoflux.adminapi.model.EventStatus;
import com.chronoflux.adminapi.model.LiveEvent;
import com.chronoflux.adminapi.repository.EventSchemaRepository;
import com.chronoflux.adminapi.repository.LiveEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveEventServiceTest {

    @Mock
    private LiveEventRepository eventRepository;

    @Mock
    private EventSchemaRepository schemaRepository;

    @Mock
    private LiveEventMapper eventMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    private LiveEventService liveEventService;

    @BeforeEach
    void setUp() {
        liveEventService = new LiveEventService(
                eventRepository,
                schemaRepository,
                eventMapper,
                new ObjectMapper(),
                redisTemplate
        );
    }

    @Test
    void createEvent_DirectActiveStatus_ThrowsIllegalArgumentException() {
        LiveEventCreateDTO createDTO = new LiveEventCreateDTO();
        createDTO.setTitle("Flash Sale");
        createDTO.setStatus(EventStatus.ACTIVE); // Invariant guard: direct transition to ACTIVE forbidden

        assertThrows(IllegalArgumentException.class, () -> liveEventService.createEvent(createDTO));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_ScheduledStatus_SavesSuccessfully() {
        LiveEventCreateDTO createDTO = new LiveEventCreateDTO();
        createDTO.setTitle("Weekend Double XP");
        createDTO.setStatus(EventStatus.SCHEDULED);
        createDTO.setActiveFrom(Instant.now().plusSeconds(3600));

        LiveEvent entity = new LiveEvent();
        entity.setStatus(EventStatus.SCHEDULED);

        when(eventMapper.toEntity(createDTO)).thenReturn(entity);
        when(eventRepository.save(entity)).thenReturn(entity);

        liveEventService.createEvent(createDTO);

        verify(eventRepository, times(1)).save(entity);
    }

    @Test
    void updateEvent_TransitionToActiveDirectly_ThrowsIllegalArgumentException() {
        UUID eventId = UUID.randomUUID();
        LiveEvent existingEvent = new LiveEvent();
        existingEvent.setStatus(EventStatus.SCHEDULED);

        LiveEventUpdateDTO updateDTO = new LiveEventUpdateDTO();
        updateDTO.setStatus(EventStatus.ACTIVE); // Invariant guard

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

        assertThrows(IllegalArgumentException.class, () -> liveEventService.updateEvent(eventId, updateDTO));
        verify(eventRepository, never()).save(existingEvent);
    }
}
