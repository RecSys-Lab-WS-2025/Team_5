package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.UserPreferenceOffsetEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface R2dbcUserPreferenceOffsetRepository extends ReactiveCrudRepository<UserPreferenceOffsetEntity, Long> {
    Mono<UserPreferenceOffsetEntity> findByUserIdAndEmotionAndCategory(Long userId, String emotion, String category);
}
