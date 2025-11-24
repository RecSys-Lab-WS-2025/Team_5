package de.tum.moodtrip_backend.core.model;

public record EnrichedPoi(
        POI poi,
        String displayName,
        String description,
        String imageUrl
) {
}
