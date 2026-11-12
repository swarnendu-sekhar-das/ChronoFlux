package com.example.edge_api.service;

import com.example.edge_api.domain.LiveEvent;
import com.example.edge_api.dto.LiveEventResponseDTO;
import com.example.edge_api.mapper.LiveEventMapper;
import com.example.edge_api.repository.LiveEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveEventServiceUnitTest {

    @Mock
    private LiveEventRepository repository;

    @Mock
    private LiveEventMapper mapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private LiveEventService service;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service = new LiveEventService(
                repository,
                mapper,
                redisTemplate,
                objectMapper
        );
    }

    @Test
    void getActiveEvents_CacheHit_ReturnsFromRedisWithoutHittingPostgres() {
        UUID eventId = UUID.randomUUID();
        String json = "{\"id\":\"" + eventId + "\",\"title\":\"Boss Raid\",\"status\":\"ACTIVE\"}";

        when(setOperations.members("active_event_ids")).thenReturn(Set.of(eventId.toString()));
        when(valueOperations.multiGet(List.of("events::" + eventId))).thenReturn(List.of(json));

        List<LiveEventResponseDTO> results = service.getActiveEvents();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Boss Raid", results.get(0).getTitle());
        // Verify Postgres was NEVER touched
        verify(repository, never()).findByStatus(any());
        verify(repository, never()).findAllById(any());
    }

    @Test
    void getActiveEvents_PartialCacheMiss_HealsFromPostgres() {
        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        String json1 = "{\"id\":\"" + event1 + "\",\"title\":\"Event 1\",\"status\":\"ACTIVE\"}";

        // event1 is cached, event2 was evicted (null in multiGet)
        when(setOperations.members("active_event_ids")).thenReturn(new LinkedHashSet<>(List.of(event1.toString(), event2.toString())));
        when(valueOperations.multiGet(List.of("events::" + event1, "events::" + event2))).thenReturn(Arrays.asList(json1, null));

        LiveEvent mockDbEvent = mock(LiveEvent.class);
        when(mockDbEvent.getId()).thenReturn(event2);
        when(mockDbEvent.getStatus()).thenReturn("ACTIVE");

        LiveEventResponseDTO dto2 = new LiveEventResponseDTO();
        dto2.setId(event2);
        dto2.setTitle("Event 2");
        dto2.setStatus("ACTIVE");

        when(repository.findAllById(List.of(event2))).thenReturn(List.of(mockDbEvent));
        when(mapper.toDTOList(List.of(mockDbEvent))).thenReturn(List.of(dto2));

        List<LiveEventResponseDTO> results = service.getActiveEvents();

        assertEquals(2, results.size());
        // Verify cache healing backfilled Redis for event2
        verify(valueOperations, times(1)).set(eq("events::" + event2), any(), any());
    }

    @Test
    void getActiveEvents_PartialCacheMiss_WhenDbEventIsArchived_FiltersOutAndCleansStaleActiveId() {
        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();
        String json1 = "{\"id\":\"" + event1 + "\",\"title\":\"Event 1\",\"status\":\"ACTIVE\"}";

        // event1 is cached, event2 was evicted/missing from Redis string cache
        when(setOperations.members("active_event_ids")).thenReturn(new LinkedHashSet<>(List.of(event1.toString(), event2.toString())));
        when(valueOperations.multiGet(List.of("events::" + event1, "events::" + event2))).thenReturn(Arrays.asList(json1, null));

        LiveEvent mockDbEvent = mock(LiveEvent.class);
        when(mockDbEvent.getId()).thenReturn(event2);
        when(mockDbEvent.getStatus()).thenReturn("ARCHIVED");

        LiveEventResponseDTO dto2 = new LiveEventResponseDTO();
        dto2.setId(event2);
        dto2.setTitle("Event 2");
        dto2.setStatus("ARCHIVED");

        when(repository.findAllById(List.of(event2))).thenReturn(List.of(mockDbEvent));
        when(mapper.toDTOList(List.of(mockDbEvent))).thenReturn(List.of(dto2));

        List<LiveEventResponseDTO> results = service.getActiveEvents();

        // Stale ARCHIVED event is filtered out; only ACTIVE event1 returned
        assertEquals(1, results.size());
        assertEquals("Event 1", results.get(0).getTitle());

        // Stale event2 ID is purged from active_event_ids
        verify(setOperations, times(1)).remove("active_event_ids", event2.toString());
        verify(valueOperations, never()).set(eq("events::" + event2), any(), any());
    }

    @Test
    void getActiveEvents_ColdStart_AcquiresLockAndRebuildsCacheWithBoundedTTL() {
        UUID eventId = UUID.randomUUID();
        when(setOperations.members("active_event_ids")).thenReturn(Collections.emptySet());
        when(valueOperations.setIfAbsent(eq("lock:rebuild_active_events"), eq("locked"), any(java.time.Duration.class)))
                .thenReturn(true);

        LiveEvent mockEvent = mock(LiveEvent.class);
        when(mockEvent.getId()).thenReturn(eventId);

        LiveEventResponseDTO dto = new LiveEventResponseDTO();
        dto.setId(eventId);
        dto.setTitle("Cold Start Event");
        dto.setStatus("ACTIVE");

        when(repository.findByStatus("ACTIVE")).thenReturn(List.of(mockEvent));
        when(mapper.toDTOList(List.of(mockEvent))).thenReturn(List.of(dto));

        List<LiveEventResponseDTO> results = service.getActiveEvents();

        assertEquals(1, results.size());
        assertEquals("Cold Start Event", results.get(0).getTitle());

        // Verify Redis cache is populated with 3-minute TTL on active set and lock is released
        verify(setOperations, times(1)).add("active_event_ids", eventId.toString());
        verify(redisTemplate, times(1)).expire(eq("active_event_ids"), eq(java.time.Duration.ofMinutes(3)));
        verify(redisTemplate, times(1)).delete("lock:rebuild_active_events");
    }

    @Test
    void getActiveEvents_WhenCachedEventIsPastActiveUntil_FiltersOutExpiredEvent() {
        UUID activeId = UUID.randomUUID();
        UUID expiredId = UUID.randomUUID();

        // One event is still active, one event has passed activeUntil
        String futureTime = java.time.Instant.now().plusSeconds(3600).toString();
        String pastTime = java.time.Instant.now().minusSeconds(3600).toString();

        String jsonActive = "{\"id\":\"" + activeId + "\",\"title\":\"Active Event\",\"status\":\"ACTIVE\",\"activeUntil\":\"" + futureTime + "\"}";
        String jsonExpired = "{\"id\":\"" + expiredId + "\",\"title\":\"Expired Event\",\"status\":\"ACTIVE\",\"activeUntil\":\"" + pastTime + "\"}";

        when(setOperations.members("active_event_ids")).thenReturn(new LinkedHashSet<>(List.of(activeId.toString(), expiredId.toString())));
        when(valueOperations.multiGet(List.of("events::" + activeId, "events::" + expiredId)))
                .thenReturn(List.of(jsonActive, jsonExpired));

        List<LiveEventResponseDTO> results = service.getActiveEvents();

        // Expired event filtered out defensively by timestamp check
        assertEquals(1, results.size());
        assertEquals("Active Event", results.get(0).getTitle());
    }
}
