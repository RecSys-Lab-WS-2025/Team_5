package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

public record UserPreferenceOffset(
        Long id,
        Long userId,
        Emotion emotion,
        PoiCategory category,
        Double userPreferenceOffset,
        Long count,
        LocalDateTime updatedAt
) {
    public static UserPreferenceOffset initial(Long userId, Emotion emotion, PoiCategory category, LocalDateTime now) {
        return new UserPreferenceOffset(null, userId, emotion, category, 0.0, 0L, now);
    }

    public UserPreferenceOffset withUserPreferenceOffset(Double newUserPreferenceOffset) {
        return new UserPreferenceOffset(id, userId, emotion, category, newUserPreferenceOffset, count, updatedAt);
    }

    public UserPreferenceOffset withCount(Long newCount) {
        return new UserPreferenceOffset(id, userId, emotion, category, userPreferenceOffset, newCount, updatedAt);
    }

    public UserPreferenceOffset withUpdatedAt(LocalDateTime newUpdatedAt) {
        return new UserPreferenceOffset(id, userId, emotion, category, userPreferenceOffset, count, newUpdatedAt);
    }
}
