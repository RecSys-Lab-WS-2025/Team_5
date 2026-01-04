package de.tum.moodtrip_backend.core.model;

public record UserPreferenceOffsetUpdateResult(
        UserPreferenceOffset offset,
        double alpha,
        double predictedScore,
        double error
) {
}
