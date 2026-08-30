package com.chronoflux.adminapi.dto;

import jakarta.validation.constraints.NotNull;

public class WebhookToggleDTO {
    
    @NotNull(message = "isActive is required")
    private Boolean isActive;

    public WebhookToggleDTO() {}

    public WebhookToggleDTO(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
