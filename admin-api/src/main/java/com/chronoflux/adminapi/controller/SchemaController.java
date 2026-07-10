package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.model.EventSchema;
import com.chronoflux.adminapi.repository.EventSchemaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/schemas")
public class SchemaController {

    private final EventSchemaRepository schemaRepository;

    public SchemaController(EventSchemaRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    @GetMapping
    public ResponseEntity<List<EventSchema>> getAllSchemas() {
        return ResponseEntity.ok(schemaRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<EventSchema> createSchema(@RequestBody EventSchema schema) {
        EventSchema savedSchema = schemaRepository.save(schema);
        return new ResponseEntity<>(savedSchema, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventSchema> getSchemaById(@PathVariable UUID id) {
        Optional<EventSchema> schema = schemaRepository.findById(id);
        return schema.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
