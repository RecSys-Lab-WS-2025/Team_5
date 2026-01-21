package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.EmotionCategoryScoreEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface R2dbcEmotionCategoryScoreRepository extends ReactiveCrudRepository<EmotionCategoryScoreEntity, Long> {
    Mono<EmotionCategoryScoreEntity> findByEmotionAndCategory(String emotion, String category);

    @Query("SELECT * FROM emotion_category_scores WHERE emotion = :emotion AND category = :category FOR UPDATE")
    Mono<EmotionCategoryScoreEntity> findByEmotionAndCategoryForUpdate(String emotion, String category);
}
