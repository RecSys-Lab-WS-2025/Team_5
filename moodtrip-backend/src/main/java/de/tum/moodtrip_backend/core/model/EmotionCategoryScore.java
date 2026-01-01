package de.tum.moodtrip_backend.core.model;

public record EmotionCategoryScore(
        Long id,
        Emotion emotion,
        PoiCategory category,
        Double score,
        Long ratingCount
) {
    public EmotionCategoryScore withScore(Double newScore) {
        return new EmotionCategoryScore(id, emotion, category, newScore, ratingCount);
    }
    public EmotionCategoryScore withCount(Long newRatingCount) {
        return new EmotionCategoryScore(id, emotion, category, score, newRatingCount);
    }
}
