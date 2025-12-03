package de.tum.moodtrip_backend.api.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SurveyRequest(
        @NotNull(message = "Latitude cannot be null")
        Double latitude,

        @NotNull(message = "Longitude cannot be null")
        Double longitude,
        
        @NotNull(message = "Range meters cannot be null")
        @Positive(message = "Range meters must be positive")
        Integer rangeMeters,
        
        @NotNull(message = "Start date cannot be null")
        LocalDate startDate,
        
        @NotNull(message = "End date cannot be null")
        LocalDate endDate,
        
        List<String> poiCategories
) {
    public SurveyRequest {
        poiCategories = poiCategories == null ? Collections.emptyList() : Collections.unmodifiableList(poiCategories);
        
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
    }
}
