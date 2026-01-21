package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.UserPreferenceOffsetEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface R2dbcUserPreferenceOffsetRepository extends ReactiveCrudRepository<UserPreferenceOffsetEntity, Long> {
    Mono<UserPreferenceOffsetEntity> findByUserIdAndEmotionAndCategory(Long userId, String emotion, String category);

    @Query("SELECT * FROM user_preference_offsets WHERE user_id = :userId AND emotion = :emotion AND category = :category FOR UPDATE")
    Mono<UserPreferenceOffsetEntity> findByUserIdAndEmotionAndCategoryForUpdate(Long userId, String emotion, String category);

    @Query("INSERT INTO user_preference_offsets (user_id, emotion, category, preference_offset, count, updated_at) " +
            "VALUES (:userId, :emotion, :category, :preferenceOffset, :count, :updatedAt) " +
            "ON CONFLICT (user_id, emotion, category) DO NOTHING RETURNING *")
    Mono<UserPreferenceOffsetEntity> insertIfAbsent(Long userId,
                                                    String emotion,
                                                    String category,
                                                    Double preferenceOffset,
                                                    Long count,
                                                    LocalDateTime updatedAt);
}
