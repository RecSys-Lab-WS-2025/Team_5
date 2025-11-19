package de.tum.moodtrip_backend.adapter_database.adapter;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.adapter_database.mapper.RouteRecommendationMapper;
import de.tum.moodtrip_backend.adapter_database.repository.R2dbcRouteRecommendationRepository;
import de.tum.moodtrip_backend.core.model.RouteRecommendationDomain;
import de.tum.moodtrip_backend.core.port.RouteRecommendationPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DatabaseRouteRecommendationAdapter implements RouteRecommendationPort {

    private final R2dbcRouteRecommendationRepository routeRecommendationRepository;
    private final RouteRecommendationMapper routeRecommendationMapper;

    public DatabaseRouteRecommendationAdapter(R2dbcRouteRecommendationRepository routeRecommendationRepository,
                                              RouteRecommendationMapper routeRecommendationMapper) {
        this.routeRecommendationRepository = routeRecommendationRepository;
        this.routeRecommendationMapper = routeRecommendationMapper;
    }

    @Override
    public Mono<RouteRecommendationDomain> save(RouteRecommendationDomain routeRecommendation) {
        return Mono.just(routeRecommendation)
                .map(routeRecommendationMapper::toEntity)
                .flatMap(routeRecommendationRepository::save)
                .map(routeRecommendationMapper::toDomain);
    }

    @Override
    public Mono<RouteRecommendationDomain> findById(Long id) {
        return routeRecommendationRepository.findById(id)
                .map(routeRecommendationMapper::toDomain);
    }

    @Override
    public Flux<RouteRecommendationDomain> findByConversationId(Long conversationId) {
        return routeRecommendationRepository.findByConversationIdOrderByCreatedAtDesc(conversationId)
                .map(routeRecommendationMapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return routeRecommendationRepository.deleteById(id);
    }
}
