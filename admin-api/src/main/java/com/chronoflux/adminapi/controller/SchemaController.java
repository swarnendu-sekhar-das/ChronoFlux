package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.dto.EventSchemaCreateDTO;
import com.chronoflux.adminapi.dto.EventSchemaDTO;
import com.chronoflux.adminapi.service.EventSchemaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/schemas")
public class SchemaController {

    private final EventSchemaService schemaService;

    public SchemaController(EventSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @GetMapping
    public ResponseEntity<List<EventSchemaDTO>> getAllSchemas() {
        return ResponseEntity.ok(schemaService.getAllSchemas());
    }

    @PostMapping
    public ResponseEntity<EventSchemaDTO> createSchema(@RequestBody EventSchemaCreateDTO schema) {
        EventSchemaDTO savedSchema = schemaService.createSchema(schema);
        return new ResponseEntity<>(savedSchema, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventSchemaDTO> getSchemaById(@PathVariable UUID id) {
        Optional<EventSchemaDTO> schema = schemaService.getSchemaById(id);
        return schema.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
