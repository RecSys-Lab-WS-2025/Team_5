package de.tum.moodtrip_backend.core.model;

public record ScoredPoi(
        Poi poi,
        PoiScore score
) {
}
