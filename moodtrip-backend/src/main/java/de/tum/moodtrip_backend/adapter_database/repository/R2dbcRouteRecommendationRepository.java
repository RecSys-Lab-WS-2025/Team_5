package de.tum.moodtrip_backend.adapter_database.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import de.tum.moodtrip_backend.adapter_database.entity.RouteRecommendationEntity;
import reactor.core.publisher.Flux;

@Repository
public interface R2dbcRouteRecommendationRepository extends ReactiveCrudRepository<RouteRecommendationEntity, Long> {
    Flux<RouteRecommendationEntity> findByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
