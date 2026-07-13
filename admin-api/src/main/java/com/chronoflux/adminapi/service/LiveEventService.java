package com.chronoflux.adminapi.service;

import com.chronoflux.adminapi.dto.LiveEventCreateDTO;
import com.chronoflux.adminapi.dto.LiveEventDTO;
import com.chronoflux.adminapi.dto.LiveEventUpdateDTO;
import com.chronoflux.adminapi.mapper.LiveEventMapper;
import com.chronoflux.adminapi.model.LiveEvent;
import com.chronoflux.adminapi.repository.LiveEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LiveEventService {

    private final LiveEventRepository eventRepository;
    private final LiveEventMapper eventMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    public LiveEventService(LiveEventRepository eventRepository, LiveEventMapper eventMapper, org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.redisTemplate = redisTemplate;
    }

    private void invalidateCache() {
        redisTemplate.delete("activeEvents::all");
    }

    public List<LiveEventDTO> getAllEvents() {
        return eventMapper.toDTOList(eventRepository.findAll());
    }

    @Transactional
    public LiveEventDTO createEvent(LiveEventCreateDTO createDTO) {
        LiveEvent event = eventMapper.toEntity(createDTO);
        LiveEvent savedEvent = eventRepository.save(event);
        invalidateCache();
        return eventMapper.toDTO(savedEvent);
    }

    public Optional<LiveEventDTO> getEventById(UUID id) {
        return eventRepository.findById(id).map(eventMapper::toDTO);
    }

    @Transactional
    public Optional<LiveEventDTO> updateEvent(UUID id, LiveEventUpdateDTO updateDTO) {
        return eventRepository.findById(id).map(event -> {
            event.setTitle(updateDTO.getTitle());
            event.setStatus(updateDTO.getStatus());
            event.setPayload(updateDTO.getPayload());
            event.setActiveFrom(updateDTO.getActiveFrom());
            event.setActiveUntil(updateDTO.getActiveUntil());
            
            LiveEvent updatedEvent = eventRepository.save(event);
            invalidateCache();
            return eventMapper.toDTO(updatedEvent);
        });
    }

    @Transactional
    public boolean deleteEvent(UUID id) {
        if (!eventRepository.existsById(id)) {
            return false;
        }
        eventRepository.deleteById(id);
        invalidateCache();
        return true;
    }
}
