package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.EmotionCategoryScoreEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface R2dbcEmotionCategoryScoreRepository extends ReactiveCrudRepository<EmotionCategoryScoreEntity, Long> {
    Mono<EmotionCategoryScoreEntity> findByEmotionAndCategory(String emotion, String category);
}
