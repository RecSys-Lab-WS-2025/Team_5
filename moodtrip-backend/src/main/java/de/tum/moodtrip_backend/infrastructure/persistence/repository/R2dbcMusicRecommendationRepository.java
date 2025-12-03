package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.MusicRecommendationEntity;
import reactor.core.publisher.Flux;

@Repository
public interface R2dbcMusicRecommendationRepository extends ReactiveCrudRepository<MusicRecommendationEntity, Long> {
    Flux<MusicRecommendationEntity> findByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
