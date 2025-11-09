package de.tum.moodtrip_backend.repository;

import de.tum.moodtrip_backend.model.Recommendation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface RecommendationRepository extends ReactiveCrudRepository<Recommendation, Long> {
    Flux<Recommendation> findByConversationId(Long conversationId);
    Flux<Recommendation> findByConversationIdAndType(Long conversationId, String type);
}

