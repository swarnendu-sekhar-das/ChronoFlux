package com.example.edge_api.service;

import com.example.edge_api.dto.LiveEventResponseDTO;
import com.example.edge_api.mapper.LiveEventMapper;
import com.example.edge_api.repository.LiveEventRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LiveEventService {

    private final LiveEventRepository repository;
    private final LiveEventMapper mapper;

    public LiveEventService(LiveEventRepository repository, LiveEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Cacheable(value = "activeEvents", key = "'all'")
    public List<LiveEventResponseDTO> getActiveEvents() {
        return mapper.toDTOList(repository.findByStatusOrderByCreatedAtDesc("ACTIVE"));
    }
}
