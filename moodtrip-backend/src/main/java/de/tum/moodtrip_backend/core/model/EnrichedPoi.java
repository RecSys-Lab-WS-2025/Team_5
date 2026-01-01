package de.tum.moodtrip_backend.core.model;

public record EnrichedPoi(
        Poi poi,
        String displayName,
        String description,
        String imageUrl,
        PoiCategory category
) {
}
