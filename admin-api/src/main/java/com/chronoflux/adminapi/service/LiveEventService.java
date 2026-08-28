package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.dto.LiveEventCreateDTO;
import com.chronoflux.adminapi.dto.LiveEventDTO;
import com.chronoflux.adminapi.dto.LiveEventUpdateDTO;
import com.chronoflux.adminapi.mapper.LiveEventMapper;
import com.chronoflux.adminapi.model.EventSchema;
import com.chronoflux.adminapi.model.EventStatus;
import com.chronoflux.adminapi.model.LiveEvent;
import com.chronoflux.adminapi.repository.EventSchemaRepository;
import com.chronoflux.adminapi.repository.LiveEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class LiveEventService {

    private final LiveEventRepository eventRepository;
    private final EventSchemaRepository schemaRepository;
    private final LiveEventMapper eventMapper;
    private final ObjectMapper objectMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    public LiveEventService(LiveEventRepository eventRepository, EventSchemaRepository schemaRepository, LiveEventMapper eventMapper, ObjectMapper objectMapper, org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.eventRepository = eventRepository;
        this.schemaRepository = schemaRepository;
        this.eventMapper = eventMapper;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }



    private void validatePayloadAgainstSchema(UUID schemaId, Map<String, Object> payload) {
        if (schemaId == null || payload == null) return;
        EventSchema schema = schemaRepository.findById(schemaId)
                .orElseThrow(() -> new IllegalArgumentException("Event schema not found with ID: " + schemaId));

        com.networknt.schema.JsonSchemaFactory factory = com.networknt.schema.JsonSchemaFactory.getInstance(com.networknt.schema.SpecVersion.VersionFlag.V7);
        com.networknt.schema.JsonSchema jsonSchema = factory.getSchema(objectMapper.valueToTree(schema.getSchemaDefinition()));
        java.util.Set<com.networknt.schema.ValidationMessage> errors = jsonSchema.validate(objectMapper.valueToTree(payload));

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("JSON Schema validation failed for schema '" + schema.getName() + "': " + errors);
        }
    }

    public org.springframework.data.domain.Page<LiveEventDTO> getAllEvents(org.springframework.data.domain.Pageable pageable, boolean includeArchived) {
        if (includeArchived) {
            return eventRepository.findAll(pageable).map(eventMapper::toDTO);
        }
        return eventRepository.findByStatusNot(EventStatus.ARCHIVED, pageable).map(eventMapper::toDTO);
    }

    @Transactional
    public LiveEventDTO createEvent(LiveEventCreateDTO createDTO) {
        if (createDTO.getStatus() == EventStatus.ACTIVE) {
            throw new IllegalArgumentException("Direct creation of ACTIVE events is forbidden. Set status to SCHEDULED with an activeFrom timestamp.");
        }
        if (createDTO.getSchema() != null && createDTO.getSchema().getId() != null) {
            validatePayloadAgainstSchema(createDTO.getSchema().getId(), createDTO.getPayload());
        }
        LiveEvent event = eventMapper.toEntity(createDTO);
        LiveEvent savedEvent = eventRepository.save(event);
        return eventMapper.toDTO(savedEvent);
    }

    public Optional<LiveEventDTO> getEventById(UUID id) {
        return eventRepository.findById(id).map(eventMapper::toDTO);
    }

    @Transactional
    public Optional<LiveEventDTO> updateEvent(UUID id, LiveEventUpdateDTO updateDTO) {
        return eventRepository.findById(id).map(event -> {
            if (event.getStatus() != EventStatus.ACTIVE && updateDTO.getStatus() == EventStatus.ACTIVE) {
                throw new IllegalArgumentException("Direct transition to ACTIVE is forbidden. Set status to SCHEDULED with an activeFrom timestamp.");
            }
            if (updateDTO.getPayload() != null && event.getSchema() != null) {
                validatePayloadAgainstSchema(event.getSchema().getId(), updateDTO.getPayload());
            }
            event.setTitle(updateDTO.getTitle());
            event.setStatus(updateDTO.getStatus());
            event.setPayload(updateDTO.getPayload());
            event.setActiveFrom(updateDTO.getActiveFrom());
            event.setActiveUntil(updateDTO.getActiveUntil());
            
            LiveEvent updatedEvent = eventRepository.save(event);
            LiveEventDTO dto = eventMapper.toDTO(updatedEvent);
            if (updatedEvent.getStatus() == EventStatus.ACTIVE) {
                try {
                    String json = objectMapper.writeValueAsString(dto);
                    redisTemplate.opsForValue().set("events::" + updatedEvent.getId(), json);
                } catch (Exception e) {
                    // Log error but don't fail the transaction
                }
            }
            return dto;
        });
    }

    @Transactional
    public Optional<LiveEventDTO> archiveEvent(UUID id) {
        return eventRepository.findById(id).map(event -> {
            event.setStatus(EventStatus.ARCHIVED);
            LiveEvent archivedEvent = eventRepository.save(event);
            LiveEventDTO dto = eventMapper.toDTO(archivedEvent);
            // If it was active and we archived it, remove from cache
            redisTemplate.opsForSet().remove("active_event_ids", archivedEvent.getId().toString());
            redisTemplate.delete("events::" + archivedEvent.getId());
            return dto;
        });
    }
}
