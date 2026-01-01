package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

public record PoiRating(
        Long id,
        Long userId,
        String poiId,
        PoiCategory category,
        Emotion emotion,
        Double rating,
        LocalDateTime createdAt
) {
}
