package de.tum.moodtrip_backend.core.model;

import java.util.Map;

public record Poi(
        long osmId,
        OsmType osmType,
        double latitude,
        double longitude,
        String name,
        Map<String, String> tags
) {
    public enum OsmType {
        NODE,
        WAY,
        RELATION
    }
}