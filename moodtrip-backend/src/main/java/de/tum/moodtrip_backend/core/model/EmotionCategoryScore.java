package de.tum.moodtrip_backend.core.model;

public record EmotionCategoryScore(
        Long id,
        Emotion emotion,
        PoiCategory category,
        Double score,
        Long count
) {
    public EmotionCategoryScore withScore(Double newScore) {
        return new EmotionCategoryScore(id, emotion, category, newScore, count);
    }
    public EmotionCategoryScore withCount(Long newCount) {
        return new EmotionCategoryScore(id, emotion, category, score, newCount);
    }
}
