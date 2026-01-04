package de.tum.moodtrip_backend.api.dto;

import jakarta.validation.constraints.*;

public record PoiRatingRequest(
        @NotBlank(message = "POI ID cannot be blank")
        String poiId,
        @NotBlank(message = "Category cannot be blank")
        String category,
        @NotBlank(message = "Emotion cannot be blank")
        String emotion,
        @DecimalMin(value = "0.5", message = "Rating must be at least 0.5")
        @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
        Double rating
) {
}
