package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.dto.EventSchemaCreateDTO;
import com.chronoflux.adminapi.dto.EventSchemaDTO;
import com.chronoflux.adminapi.service.EventSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/schemas")
@Tag(name = "Event Schemas", description = "Endpoints for managing Event Schemas")
public class SchemaController {

    private final EventSchemaService schemaService;

    public SchemaController(EventSchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @Operation(summary = "Get all schemas", description = "Retrieves a list of all event schemas")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    @GetMapping
    public ResponseEntity<List<EventSchemaDTO>> getAllSchemas(
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(schemaService.getAllSchemas(includeArchived));
    }

    @Operation(summary = "Create a new schema", description = "Creates a new event schema structure")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Schema successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid schema payload")
    })
    @PostMapping
    public ResponseEntity<EventSchemaDTO> createSchema(@RequestBody EventSchemaCreateDTO schema) {
        EventSchemaDTO savedSchema = schemaService.createSchema(schema);
        return new ResponseEntity<>(savedSchema, HttpStatus.CREATED);
    }

    @Operation(summary = "Get a schema by ID", description = "Retrieves details of a specific event schema")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved schema"),
            @ApiResponse(responseCode = "404", description = "Schema not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventSchemaDTO> getSchemaById(@PathVariable UUID id) {
        Optional<EventSchemaDTO> schema = schemaService.getSchemaById(id);
        return schema.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Archive a schema", description = "Marks an event schema as archived (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schema successfully archived"),
            @ApiResponse(responseCode = "404", description = "Schema not found")
    })
    @PutMapping("/{id}/archive")
    public ResponseEntity<EventSchemaDTO> archiveSchema(@PathVariable UUID id) {
        return schemaService.archiveSchema(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
