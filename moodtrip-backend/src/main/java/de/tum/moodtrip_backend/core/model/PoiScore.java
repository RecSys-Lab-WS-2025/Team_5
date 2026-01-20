package de.tum.moodtrip_backend.core.model;

import java.util.Map;

public record PoiScore(
        double finalScore,
        double categoryScore,
        double categoryBaseScore,
        boolean categoryBoostApplied,
        double tagScore,
        double distanceScore,
        double distanceMeters,
        Map<Emotion, Double> emotionContributions
) {
}
