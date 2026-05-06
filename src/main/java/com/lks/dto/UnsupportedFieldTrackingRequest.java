package com.lks.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class UnsupportedFieldTrackingRequest {
    private final Map<String, Object> unsupportedFields = new LinkedHashMap<>();

    @JsonAnySetter
    public void addUnsupportedField(String fieldName, Object value) {
        if (!"_csrf".equals(fieldName)) {
            unsupportedFields.put(fieldName, value);
        }
    }

    @JsonIgnore
    public Map<String, Object> getUnsupportedFields() {
        return unsupportedFields;
    }
}
