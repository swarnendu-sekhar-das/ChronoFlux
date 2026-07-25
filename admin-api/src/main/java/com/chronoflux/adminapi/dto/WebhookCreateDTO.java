package com.chronoflux.adminapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class WebhookCreateDTO {
    @NotNull(message = "Schema ID cannot be null")
    private UUID schemaId;

    @NotBlank(message = "Target URL cannot be empty")
    private String targetUrl;

    public UUID getSchemaId() { return schemaId; }
    public void setSchemaId(UUID schemaId) { this.schemaId = schemaId; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
}
