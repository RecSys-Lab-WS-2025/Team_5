package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.RouteRecommendationDomain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RouteRecommendationPort {
    Mono<RouteRecommendationDomain> save(RouteRecommendationDomain routeRecommendation);
    Mono<RouteRecommendationDomain> findById(Long id);
    Flux<RouteRecommendationDomain> findByConversationId(Long conversationId);
    Mono<Void> deleteById(Long id);
}
