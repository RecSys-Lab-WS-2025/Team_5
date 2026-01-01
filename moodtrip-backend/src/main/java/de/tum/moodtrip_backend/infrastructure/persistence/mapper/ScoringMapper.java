package de.tum.moodtrip_backend.infrastructure.persistence.mapper;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EmotionCategoryScore;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiRating;
import de.tum.moodtrip_backend.infrastructure.persistence.entity.EmotionCategoryScoreEntity;
import de.tum.moodtrip_backend.infrastructure.persistence.entity.PoiRatingEntity;

public class ScoringMapper {

    public static EmotionCategoryScore toDomain(EmotionCategoryScoreEntity entity) {
        if (entity == null) return null;
        return new EmotionCategoryScore(
                entity.getId(),
                Emotion.fromString(entity.getEmotion()),
                PoiCategory.fromString(entity.getCategory()),
                entity.getScore(),
                entity.getRatingCount()
        );
    }

    public static EmotionCategoryScoreEntity toEntity(EmotionCategoryScore domain) {
        if (domain == null) return null;
        return new EmotionCategoryScoreEntity(
                domain.id(),
                domain.emotion().name(),
                domain.category().name(),
                domain.score(),
                domain.ratingCount()
        );
    }

    public static PoiRating toDomain(PoiRatingEntity entity) {
        if (entity == null) return null;
        return new PoiRating(
                entity.getId(),
                entity.getUserId(),
                entity.getPoiId(),
                PoiCategory.fromString(entity.getCategory()),
                Emotion.fromString(entity.getEmotion()),
                entity.getRating(),
                entity.getCreatedAt()
        );
    }

    public static PoiRatingEntity toEntity(PoiRating domain) {
        if (domain == null) return null;
        return new PoiRatingEntity(
                domain.id(),
                domain.userId(),
                domain.poiId(),
                domain.category().name(),
                domain.emotion().name(),
                domain.rating(),
                domain.createdAt()
        );
    }
}
