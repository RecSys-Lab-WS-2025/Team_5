package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.adapter.maps.osm.adapter.OverpassAdapter;
import de.tum.moodtrip_backend.adapter.maps.osm.builder.PoiDescriptionBuilder;
import de.tum.moodtrip_backend.core.mapper.PoiRouteCoordinatesMapper;
import de.tum.moodtrip_backend.core.mapper.PoiRouteResultRouteRecommendationMapper;
import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiRouteResult;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.RouteGenerationResult;
import de.tum.moodtrip_backend.core.model.ScoredPoi;
import de.tum.moodtrip_backend.core.port.OsmPort;
import de.tum.moodtrip_backend.core.port.RouteRecommendationPort;
import de.tum.moodtrip_backend.core.port.RoutingPort;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
public class RouteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteService.class);
    private static final Duration ROUTE_TIMEOUT = Duration.ofSeconds(30);
    private static final String GENERIC_ERROR_MESSAGE = "I couldn't generate a route due to a routing service error. Please try again.";
    private static final int MAX_POI_RESULTS = 10;

    private final OsmPort osmPort;
    private final WikipediaPort wikipediaPort;
    private final RoutingPort routingPort;
    private final RouteRecommendationPort routeRecommendationPort;
    private final PoiScoringService poiScoringService;

    public RouteService(OsmPort osmPort,
                        WikipediaPort wikipediaPort,
                        RoutingPort routingPort,
                        RouteRecommendationPort routeRecommendationPort,
                        PoiScoringService poiScoringService) {
        this.osmPort = osmPort;
        this.wikipediaPort = wikipediaPort;
        this.routingPort = routingPort;
        this.routeRecommendationPort = routeRecommendationPort;
        this.poiScoringService = poiScoringService;
    }

    public Mono<RouteGenerationResult> getRoute(
            long conversationId,
            long userId,
            double lat,
            double lon,
            List<PoiCategory> poiCategories,
            int radiusMeters,
            Map<Emotion, Double> emotionWeights
    ) {
        LOGGER.info("Getting route for conversationId: {}, lat: {}, lon: {}, radius: {}", conversationId, lat, lon, radiusMeters);

        return buildRoute(userId, lat, lon, poiCategories, radiusMeters, emotionWeights)
                .timeout(ROUTE_TIMEOUT)
                .flatMap(route ->
                        routeRecommendationPort.save(PoiRouteResultRouteRecommendationMapper.toDomain(route, conversationId))
                                .doOnNext(saved -> LOGGER.info("Route generated successfully for conversationId: {}", saved.conversationId()))
                                .map(saved -> RouteGenerationResult.success(route))
                )
                .onErrorResume(ex -> {
                    LOGGER.error("Unexpected error during route generation for conversationId: {}", conversationId, ex);
                    return Mono.just(RouteGenerationResult.failure(mapErrorToUserMessage(ex)));
                });
    }

    private Mono<PoiRouteResult> buildRoute(
            long userId,
            double lat,
            double lon,
            List<PoiCategory> poiCategories,
            int radiusMeters,
            Map<Emotion, Double> emotionWeights
    ) {
        // Frontend-selected categories are intentionally ignored by the Overpass adapter for now;
        // keep plumbing intact so we can re-enable client-side filtering later.
        Flux<Poi> poiFlux = osmPort.findAmenitiesAround(lat, lon, poiCategories, radiusMeters)
                .cache();

        return poiScoringService.scoreAndRank(poiFlux, userId, emotionWeights, lat, lon, MAX_POI_RESULTS)
                .flatMap(scoredPois -> {
                    if (scoredPois.isEmpty()) {
                        return Mono.error(new NotEnoughPoisException("No POIs found after categorization and scoring"));
                    }
                    return enrichPois(scoredPois)
                            .collectList()
                            .flatMap(enrichedPois -> {
                                if (enrichedPois.size() < 2) {
                                    return Mono.error(new NotEnoughPoisException("Not enough POIs to build a route"));
                                }
                                return routingPort.calculateRoute(PoiRouteCoordinatesMapper.toCoordinates(
                                                enrichedPois.stream().map(EnrichedPoi::poi).toList()
                                        ))
                                        .map(route -> new PoiRouteResult(enrichedPois, route));
                            });
                });
    }

    private Flux<EnrichedPoi> enrichPois(List<ScoredPoi> scoredPois) {
        return Flux.fromIterable(scoredPois)
                .flatMap(scoredPoi -> {
                    var poi = scoredPoi.poi();
                    return wikipediaPort.fetchSummaryForTag(poi.tags().get("wikipedia"))
                            .defaultIfEmpty("")
                            .zipWith(
                                    wikipediaPort.fetchImageUrl(
                                                    poi.tags().get("image"),
                                                    poi.tags().get("wikipedia"),
                                                    poi.tags().get("wikidata"),
                                                    poi.tags().get("wikimedia_commons")
                                            )
                                            .defaultIfEmpty("")
                            )
                            .map(tuple -> {
                                String summary = tuple.getT1();
                                String imageUrl = tuple.getT2();

                                String displayName = PoiDescriptionBuilder.buildDisplayName(poi);
                                String description = PoiDescriptionBuilder.buildShortDescription(poi, summary);

                                return new EnrichedPoi(poi, displayName, description, imageUrl, poi.category(), scoredPoi.score());
                            });
                });
    }

    private String mapErrorToUserMessage(Throwable error) {
        if (error == null) {
            return GENERIC_ERROR_MESSAGE;
        }
        if (hasCause(error, NotEnoughPoisException.class)) {
            return "I couldn't find enough interesting places nearby to build a route. Please try a larger radius or a different area.";
        }
        if (hasCause(error, OverpassAdapter.OverpassUnavailableException.class)) {
            return "The map service timed out while fetching places. Please try again in a moment.";
        }
        if (isTimeout(error)) {
            return "I couldn't generate a route because the routing service timed out. Please try again.";
        }
        return GENERIC_ERROR_MESSAGE;
    }

    private boolean isTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> target) {
        Throwable current = error;
        while (current != null) {
            if (target.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class NotEnoughPoisException extends RuntimeException {
        NotEnoughPoisException(String message) {
            super(message);
        }
    }
}
