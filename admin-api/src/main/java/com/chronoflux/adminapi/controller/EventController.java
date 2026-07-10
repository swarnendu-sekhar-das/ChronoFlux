package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.model.LiveEvent;
import com.chronoflux.adminapi.repository.LiveEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/events")
public class EventController {

    private final LiveEventRepository eventRepository;

    public EventController(LiveEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public ResponseEntity<List<LiveEvent>> getAllEvents() {
        return ResponseEntity.ok(eventRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<LiveEvent> createEvent(@RequestBody LiveEvent event) {
        LiveEvent savedEvent = eventRepository.save(event);
        return new ResponseEntity<>(savedEvent, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiveEvent> getEventById(@PathVariable Long id) {
        Optional<LiveEvent> event = eventRepository.findById(id);
        return event.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LiveEvent> updateEvent(@PathVariable Long id, @RequestBody LiveEvent eventDetails) {
        Optional<LiveEvent> eventOptional = eventRepository.findById(id);

        if (eventOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LiveEvent event = eventOptional.get();
        event.setName(eventDetails.getName());
        event.setStatus(eventDetails.getStatus());
        event.setPayload(eventDetails.getPayload());
        event.setActiveFrom(eventDetails.getActiveFrom());
        event.setActiveUntil(eventDetails.getActiveUntil());
        
        LiveEvent updatedEvent = eventRepository.save(event);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
