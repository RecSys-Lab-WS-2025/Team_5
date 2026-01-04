package de.tum.moodtrip_backend.infrastructure.persistence.mapper;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffset;
import de.tum.moodtrip_backend.infrastructure.persistence.entity.UserPreferenceOffsetEntity;

public class UserPreferenceOffsetMapper {

    private UserPreferenceOffsetMapper() {
        // utility
    }

    public static UserPreferenceOffset toDomain(UserPreferenceOffsetEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserPreferenceOffset(
                entity.getId(),
                entity.getUserId(),
                Emotion.fromString(entity.getEmotion()),
                PoiCategory.fromString(entity.getCategory()),
                entity.getPreferenceOffset(),
                entity.getCount(),
                entity.getUpdatedAt()
        );
    }

    public static UserPreferenceOffsetEntity toEntity(UserPreferenceOffset domain) {
        if (domain == null) {
            return null;
        }
        return new UserPreferenceOffsetEntity(
                domain.id(),
                domain.userId(),
                domain.emotion().name(),
                domain.category().name(),
                domain.userPreferenceOffset(),
                domain.count(),
                domain.updatedAt()
        );
    }
}
