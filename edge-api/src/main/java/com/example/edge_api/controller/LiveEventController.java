package com.example.edge_api.controller;

import com.example.edge_api.dto.LiveEventResponseDTO;
import com.example.edge_api.service.LiveEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
@Tag(name = "Client Edge API", description = "High-throughput endpoints for game clients")
public class LiveEventController {

    private final LiveEventService service;

    public LiveEventController(LiveEventService service) {
        this.service = service;
    }

    @Operation(summary = "Get active events", description = "Returns all currently active live events.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved active events")
    @GetMapping("/active")
    public ResponseEntity<List<LiveEventResponseDTO>> getActiveEvents() {
        return ResponseEntity.ok(service.getActiveEvents());
    }

    @Operation(summary = "Get event by ID", description = "Returns a single event by its UUID. Cached individually in Redis.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved event")
    @GetMapping("/{id}")
    public ResponseEntity<LiveEventResponseDTO> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getEventById(id));
    }
}
