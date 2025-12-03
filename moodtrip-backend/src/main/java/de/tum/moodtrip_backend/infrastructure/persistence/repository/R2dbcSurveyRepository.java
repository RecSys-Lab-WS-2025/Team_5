package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.SurveyEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface R2dbcSurveyRepository extends ReactiveCrudRepository<SurveyEntity, Long> {

    Flux<SurveyEntity> findByUserId(Long userId);

    Flux<SurveyEntity> findByConversationId(Long conversationId);

    @Query("SELECT * FROM surveys WHERE user_id = :userId ORDER BY created_at DESC LIMIT 1")
    Mono<SurveyEntity> findLatestByUserId(Long userId);
}
