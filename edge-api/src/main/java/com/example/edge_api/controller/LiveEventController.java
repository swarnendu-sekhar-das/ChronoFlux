package com.example.edge_api.controller;

import com.example.edge_api.dto.LiveEventResponseDTO;
import com.example.edge_api.service.LiveEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class LiveEventController {

    private final LiveEventService service;

    public LiveEventController(LiveEventService service) {
        this.service = service;
    }

    @GetMapping("/active")
    public List<LiveEventResponseDTO> getActiveEvents() {
        return service.getActiveEvents();
    }
}
