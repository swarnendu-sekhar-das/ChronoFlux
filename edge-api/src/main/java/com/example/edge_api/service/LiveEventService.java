package com.example.edge_api.service;

import com.example.edge_api.domain.LiveEvent;
import com.example.edge_api.repository.LiveEventRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LiveEventService {

    private final LiveEventRepository repository;

    public LiveEventService(LiveEventRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "activeEvents", key = "'all'")
    public List<LiveEvent> getActiveEvents() {
        return repository.findByStatus("ACTIVE");
    }
}
