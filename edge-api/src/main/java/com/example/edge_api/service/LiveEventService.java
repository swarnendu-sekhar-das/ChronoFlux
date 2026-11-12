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

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LiveEventService {

    private static final Logger logger = LoggerFactory.getLogger(LiveEventService.class);
    private static final String ACTIVE_IDS_KEY = "active_event_ids";
    private static final String EVENT_KEY_PREFIX = "events::";
    private static final String REBUILD_LOCK_KEY = "lock:rebuild_active_events";

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
     * Fast Path: SMEMBERS active_event_ids -> MGET events::id1, events::id2 -> return
     * Cold Start: SET is empty -> acquire distributed mutex (SET NX EX) -> rebuild from DB
     * Lock Contenders: sleep with randomized jitter (30-80ms) and re-read warmed cache
     */
    public List<LiveEventResponseDTO> getActiveEvents() {
        Set<String> activeIds = null;
        try {
            activeIds = redisTemplate.opsForSet().members(ACTIVE_IDS_KEY);
        } catch (Exception e) {
            logger.warn("Redis unavailable during active event lookup. Falling back to PostgreSQL directly: {}", e.getMessage());
            return mapper.toDTOList(repository.findByStatus("ACTIVE"));
        }

        // Cold Start / Cache Miss
        if (activeIds == null || activeIds.isEmpty()) {
            return handleCacheStampedeAndRebuild();
        }

        return fetchEventsFromCache(activeIds);
    }

    /**
     * Guards against Cache Stampede (Thundering Herd) under high concurrency.
     * Uses Redis SET NX with a 3-second TTL as a distributed lock.
     * Only one thread hits PostgreSQL to rebuild the cache; waiting threads sleep with jitter.
     */
    private List<LiveEventResponseDTO> handleCacheStampedeAndRebuild() {
        Boolean acquired = false;
        try {
            acquired = redisTemplate.opsForValue().setIfAbsent(REBUILD_LOCK_KEY, "locked", Duration.ofSeconds(3));
        } catch (Exception e) {
            logger.warn("Failed to acquire rebuild lock from Redis: {}. Querying database directly.", e.getMessage());
            return mapper.toDTOList(repository.findByStatus("ACTIVE"));
        }

        if (Boolean.TRUE.equals(acquired)) {
            try {
                logger.info("Acquired rebuild lock '{}'. Rebuilding active events cache from Postgres.", REBUILD_LOCK_KEY);
                return rebuildCacheFromDb();
            } finally {
                try {
                    redisTemplate.delete(REBUILD_LOCK_KEY);
                } catch (Exception e) {
                    logger.warn("Failed to release rebuild lock: {}", e.getMessage());
                }
            }
        } else {
            // Lock held by another thread. Sleep with randomized jitter to prevent secondary stampedes.
            try {
                long jitter = 30 + (long) (Math.random() * 50);
                Thread.sleep(jitter);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Retry reading from Redis now that the lock holder finished warming
            Set<String> retryIds = null;
            try {
                retryIds = redisTemplate.opsForSet().members(ACTIVE_IDS_KEY);
            } catch (Exception e) {
                // Ignore and fall through to DB
            }

            if (retryIds != null && !retryIds.isEmpty()) {
                return fetchEventsFromCache(retryIds);
            }

            // Fallback: direct database read
            logger.info("Cache still empty after backoff. Falling back to Postgres read.");
            return mapper.toDTOList(repository.findByStatus("ACTIVE"));
        }
    }

    /**
     * Batch-fetches individual event JSONs using MGET.
     * Handles Partial Cache Miss: if any individual key expired or was evicted,
     * fetches only the missing records from PostgreSQL and heals the cache.
     */
    private List<LiveEventResponseDTO> fetchEventsFromCache(Set<String> activeIds) {
        List<String> idList = new ArrayList<>(activeIds);
        List<String> keys = idList.stream()
                .map(id -> EVENT_KEY_PREFIX + id)
                .collect(Collectors.toList());

        List<String> cachedJsons = null;
        try {
            cachedJsons = redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            logger.warn("Redis MGET failed, falling back to DB: {}", e.getMessage());
            return mapper.toDTOList(repository.findByStatus("ACTIVE"));
        }

        List<LiveEventResponseDTO> results = new ArrayList<>();
        List<UUID> missingUuids = new ArrayList<>();

        for (int i = 0; i < idList.size(); i++) {
            String json = (cachedJsons != null && i < cachedJsons.size()) ? cachedJsons.get(i) : null;
            String eventIdStr = idList.get(i);

            if (json != null) {
                try {
                    LiveEventResponseDTO dto = objectMapper.readValue(json, LiveEventResponseDTO.class);
                    // Defensive guard: if an event expired past activeUntil, filter it out from active results
                    if (dto.getActiveUntil() != null) {
                        try {
                            if (Instant.parse(dto.getActiveUntil()).isBefore(Instant.now())) {
                                logger.debug("Skipping cached event {} because activeUntil ({}) has elapsed",
                                        eventIdStr, dto.getActiveUntil());
                                continue;
                            }
                        } catch (Exception ignored) {}
                    }
                    results.add(dto);
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to deserialize cached event {}, marking for DB re-fetch.", eventIdStr);
                    try {
                        missingUuids.add(UUID.fromString(eventIdStr));
                    } catch (Exception ignored) {}
                }
            } else {
                // Partial Cache Miss: Individual event key was evicted under memory pressure
                try {
                    missingUuids.add(UUID.fromString(eventIdStr));
                } catch (Exception ignored) {}
            }
        }

        // Cache Healing: Recover any missing events from PostgreSQL and backfill Redis
        if (!missingUuids.isEmpty()) {
            logger.info("Detected {} partial cache misses. Healing cache from Postgres.", missingUuids.size());
            List<LiveEvent> missingEvents = repository.findAllById(missingUuids);
            List<LiveEventResponseDTO> missingDtos = mapper.toDTOList(missingEvents);

            Set<UUID> foundUuids = new HashSet<>();
            for (int i = 0; i < missingEvents.size(); i++) {
                LiveEvent event = missingEvents.get(i);
                LiveEventResponseDTO dto = missingDtos.get(i);
                foundUuids.add(event.getId());

                // Read-repair: verify that the event is still genuinely ACTIVE in PostgreSQL
                if ("ACTIVE".equalsIgnoreCase(event.getStatus())) {
                    boolean expired = false;
                    if (dto.getActiveUntil() != null) {
                        try {
                            if (Instant.parse(dto.getActiveUntil()).isBefore(Instant.now())) {
                                expired = true;
                            }
                        } catch (Exception ignored) {}
                    }
                    if (!expired) {
                        results.add(dto);
                        try {
                            String json = objectMapper.writeValueAsString(dto);
                            redisTemplate.opsForValue().set(EVENT_KEY_PREFIX + event.getId(), json, Duration.ofHours(24));
                        } catch (Exception e) {
                            logger.warn("Failed to heal Redis cache key for event {}", event.getId(), e);
                        }
                    }
                } else {
                    // Stale ID in active_event_ids: event was ARCHIVED or EXPIRED in DB.
                    // Lazily purge the stale ID from the active set so future requests don't miss.
                    try {
                        redisTemplate.opsForSet().remove(ACTIVE_IDS_KEY, event.getId().toString());
                    } catch (Exception e) {
                        logger.warn("Failed to remove stale event ID {} from active set: {}", event.getId(), e.getMessage());
                    }
                }
            }

            // Also purge any IDs that were deleted from PostgreSQL entirely
            for (UUID missingId : missingUuids) {
                if (!foundUuids.contains(missingId)) {
                    try {
                        redisTemplate.opsForSet().remove(ACTIVE_IDS_KEY, missingId.toString());
                    } catch (Exception ignored) {}
                }
            }
        }

        return results;
    }

    /**
     * Cold start cache builder: queries Postgres and populates Redis SET + individual string keys.
     */
    private List<LiveEventResponseDTO> rebuildCacheFromDb() {
        List<LiveEvent> events = repository.findByStatus("ACTIVE");
        List<LiveEventResponseDTO> dtos = mapper.toDTOList(events);

        for (int i = 0; i < events.size(); i++) {
            String id = events.get(i).getId().toString();
            try {
                String json = objectMapper.writeValueAsString(dtos.get(i));
                redisTemplate.opsForSet().add(ACTIVE_IDS_KEY, id);
                redisTemplate.opsForValue().set(EVENT_KEY_PREFIX + id, json, Duration.ofHours(24));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize event {} for Redis cache.", id, e);
            }
        }

        // Set bounded TTL on the active_event_ids set (3 minutes) so any stale membership self-heals
        // even if a manual eviction or network partition prevented cache clearing.
        try {
            redisTemplate.expire(ACTIVE_IDS_KEY, Duration.ofMinutes(3));
        } catch (Exception e) {
            logger.warn("Could not set TTL on active_event_ids: {}", e.getMessage());
        }

        return dtos;
    }

    /**
     * Individual event lookup — uses Spring @Cacheable for clean AOP caching.
     */
    @Cacheable(value = "events", key = "#eventId")
    public LiveEventResponseDTO getEventById(UUID eventId) {
        return mapper.toDTO(repository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId)));
    }
}
