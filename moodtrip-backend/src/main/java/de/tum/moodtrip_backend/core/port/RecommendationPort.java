package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.RecommendationDomain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationPort {
    Mono<RecommendationDomain> save(RecommendationDomain recommendation);
    Mono<RecommendationDomain> findById(Long id);
    Flux<RecommendationDomain> findByConversationId(Long conversationId);
    Flux<RecommendationDomain> findByConversationIdAndType(Long conversationId, String type);
    Flux<RecommendationDomain> findByType(String type);
    Mono<Long> countByConversationId(Long conversationId);
    Mono<Void> deleteById(Long id);
}
