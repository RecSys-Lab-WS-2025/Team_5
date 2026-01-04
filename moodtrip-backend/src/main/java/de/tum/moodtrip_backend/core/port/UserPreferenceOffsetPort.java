package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffset;
import reactor.core.publisher.Mono;

public interface UserPreferenceOffsetPort {
    Mono<UserPreferenceOffset> findByUserEmotionAndCategory(Long userId, Emotion emotion, PoiCategory category);

    Mono<UserPreferenceOffset> findByUserEmotionAndCategoryForUpdate(Long userId, Emotion emotion, PoiCategory category);

    Mono<UserPreferenceOffset> insertIfAbsent(UserPreferenceOffset offset);

    Mono<UserPreferenceOffset> upsert(UserPreferenceOffset offset);
}
