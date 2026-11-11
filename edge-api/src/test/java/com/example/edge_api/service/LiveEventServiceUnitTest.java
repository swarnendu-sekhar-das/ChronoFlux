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
}
