package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.MusicRecommendationDomain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MusicRecommendationPort {
    Mono<MusicRecommendationDomain> save(MusicRecommendationDomain musicRecommendation);
    Mono<MusicRecommendationDomain> findById(Long id);
    Flux<MusicRecommendationDomain> findByConversationId(Long conversationId);
    Mono<Void> deleteById(Long id);
}
