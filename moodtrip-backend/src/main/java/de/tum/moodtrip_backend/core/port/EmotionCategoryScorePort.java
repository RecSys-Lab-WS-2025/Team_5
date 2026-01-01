package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EmotionCategoryScore;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import reactor.core.publisher.Mono;

public interface EmotionCategoryScorePort {
    Mono<EmotionCategoryScore> findByEmotionAndCategory(Emotion emotion, PoiCategory category);
    Mono<EmotionCategoryScore> save(EmotionCategoryScore score);
}
