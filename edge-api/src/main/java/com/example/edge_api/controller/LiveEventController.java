package com.example.edge_api.controller;

import com.example.edge_api.dto.LiveEventResponseDTO;
import com.example.edge_api.service.LiveEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
@Tag(name = "Client Edge API", description = "High-throughput endpoints for game clients")
public class LiveEventController {

    private final LiveEventService service;

    public LiveEventController(LiveEventService service) {
        this.service = service;
    }

    @Operation(summary = "Get active events", description = "Retrieves all currently active live events. This endpoint is heavily cached to withstand 'thundering herd' traffic from millions of game clients.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved active events")
    @GetMapping("/active")
    public List<LiveEventResponseDTO> getActiveEvents() {
        return service.getActiveEvents();
    }
}
