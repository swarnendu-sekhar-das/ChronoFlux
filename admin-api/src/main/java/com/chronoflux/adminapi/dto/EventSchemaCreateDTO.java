package com.chronoflux.adminapi.dto;

import java.util.Map;

public class EventSchemaCreateDTO {
    private String name;
    private String description;
    private Map<String, Object> schemaDefinition;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, Object> getSchemaDefinition() { return schemaDefinition; }
    public void setSchemaDefinition(Map<String, Object> schemaDefinition) { this.schemaDefinition = schemaDefinition; }
}
