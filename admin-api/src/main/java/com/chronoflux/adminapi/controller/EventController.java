package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.dto.LiveEventCreateDTO;
import com.chronoflux.adminapi.dto.LiveEventDTO;
import com.chronoflux.adminapi.dto.LiveEventUpdateDTO;
import com.chronoflux.adminapi.service.LiveEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/events")
public class EventController {

    private final LiveEventService eventService;

    public EventController(LiveEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<LiveEventDTO>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @PostMapping
    public ResponseEntity<LiveEventDTO> createEvent(@RequestBody LiveEventCreateDTO event) {
        LiveEventDTO savedEvent = eventService.createEvent(event);
        return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiveEventDTO> getEventById(@PathVariable UUID id) {
        Optional<LiveEventDTO> event = eventService.getEventById(id);
        return event.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LiveEventDTO> updateEvent(@PathVariable UUID id, @RequestBody LiveEventUpdateDTO eventDetails) {
        Optional<LiveEventDTO> updatedEvent = eventService.updateEvent(id, eventDetails);
        return updatedEvent.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        if (!eventService.deleteEvent(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
