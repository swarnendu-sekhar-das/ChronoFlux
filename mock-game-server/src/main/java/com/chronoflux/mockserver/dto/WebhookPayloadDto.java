package com.chronoflux.mockserver.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

public class WebhookPayloadDto {
    private Map<String, Object> properties = new HashMap<>();

    @JsonAnySetter
    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
