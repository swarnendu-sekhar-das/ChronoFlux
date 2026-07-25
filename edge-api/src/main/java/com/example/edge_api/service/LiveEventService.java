package com.example.edge_api.service;

import com.example.edge_api.dto.LiveEventResponseDTO;
import com.example.edge_api.dto.PagedResponse;
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

    @Cacheable(value = "activeEvents", key = "'all_' + #pageable.pageNumber + '_' + #pageable.pageSize", sync = true)
    public PagedResponse<LiveEventResponseDTO> getActiveEvents(org.springframework.data.domain.Pageable pageable) {
        return new PagedResponse<>(repository.findByStatusOrderByCreatedAtDesc("ACTIVE", pageable).map(mapper::toDTO));
    }
}
