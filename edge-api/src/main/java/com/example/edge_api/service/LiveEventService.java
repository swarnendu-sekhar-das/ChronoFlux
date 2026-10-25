package com.example.edge_api.service;

import com.example.edge_api.domain.LiveEvent;
import com.example.edge_api.dto.LiveEventResponseDTO;
import com.example.edge_api.mapper.LiveEventMapper;
import com.example.edge_api.repository.LiveEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LiveEventService {

    private static final Logger logger = LoggerFactory.getLogger(LiveEventService.class);
    private static final String ACTIVE_IDS_KEY = "active_event_ids";
    private static final String EVENT_KEY_PREFIX = "events::";

    private final LiveEventRepository repository;
    private final LiveEventMapper mapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public LiveEventService(LiveEventRepository repository, LiveEventMapper mapper,
            StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns all active events using Redis SET + individual key caching.
     * 
     * Fast Path: SMEMBERS active_event_ids → MGET events::id1, events::id2 → return
     * Cold Start: SET is empty → query Postgres → populate SET + individual keys →
     * return
     */
    public List<LiveEventResponseDTO> getActiveEvents() {
        // Step 1: Read the SET of active event IDs from Redis
        Set<String> activeIds = redisTemplate.opsForSet().members(ACTIVE_IDS_KEY);

        // Step 2: If SET is empty or doesn't exist, this is a cold start — rebuild from
        // DB
        if (activeIds == null || activeIds.isEmpty()) {
            logger.info("Redis SET '{}' is empty. Rebuilding from Postgres.", ACTIVE_IDS_KEY);
            return rebuildCacheFromDb();
        }

        // Step 3: Batch-fetch all individual event JSONs using MGET
        List<String> keys = activeIds.stream()
                .map(id -> EVENT_KEY_PREFIX + id)
                .collect(Collectors.toList());
        List<String> cachedJsons = redisTemplate.opsForValue().multiGet(keys);

        // Step 4: Deserialize and return
        List<LiveEventResponseDTO> results = new ArrayList<>();
        List<String> idList = new ArrayList<>(activeIds);
        for (int i = 0; i < idList.size(); i++) {
            String json = (cachedJsons != null) ? cachedJsons.get(i) : null;
            if (json != null) {
                try {
                    results.add(objectMapper.readValue(json, LiveEventResponseDTO.class));
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to deserialize cached event {}, skipping.", idList.get(i));
                }
            }
            // MVP: If individual key is missing (partial miss), we skip it.
            // Production fix: query Postgres for the missing ID and re-cache it.
        }
        return results;
    }

    /**
     * Cold start: Query Postgres, populate the Redis SET and individual keys.
     */
    private List<LiveEventResponseDTO> rebuildCacheFromDb() {
        List<LiveEvent> events = repository.findByStatus("ACTIVE");
        List<LiveEventResponseDTO> dtos = mapper.toDTOList(events);

        for (int i = 0; i < events.size(); i++) {
            String id = events.get(i).getId().toString();
            try {
                String json = objectMapper.writeValueAsString(dtos.get(i));
                redisTemplate.opsForSet().add(ACTIVE_IDS_KEY, id);
                redisTemplate.opsForValue().set(EVENT_KEY_PREFIX + id, json);
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event {} for Redis cache.", id, e);
            }
        }
        return dtos;
    }

    /**
     * Individual event lookup — uses Spring @Cacheable for clean AOP caching.
     * Cache name "events" with key = event UUID.
     */
    @Cacheable(value = "events", key = "#eventId")
    public LiveEventResponseDTO getEventById(UUID eventId) {
        return mapper.toDTO(repository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId)));
    }
}
