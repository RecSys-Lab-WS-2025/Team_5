package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.exception.MapProviderUnavailableException;
import de.tum.moodtrip_backend.core.mapper.PoiRouteCoordinatesMapper;
import de.tum.moodtrip_backend.core.mapper.PoiRouteResultRouteRecommendationMapper;
import de.tum.moodtrip_backend.core.model.*;
import de.tum.moodtrip_backend.core.port.OsmPort;
import de.tum.moodtrip_backend.core.port.RouteRecommendationPort;
import de.tum.moodtrip_backend.core.port.RoutingPort;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import de.tum.moodtrip_backend.core.util.PoiDescriptionBuilder;
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
    private static final Duration ROUTE_TIMEOUT = Duration.ofSeconds(60);
    private static final String GENERIC_ERROR_MESSAGE = "I couldn't generate a route due to a routing service error. Please try again.";
    private static final int MAX_POI_RESULTS = 15;
    private static final int MIN_POI_RESULTS = 2;
    public static final int POI_VISITING_TIME_SECONDS = 45 * 60;
    public static final double WALKING_SPEED_MPS = 5000.0 / 3600.0;

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
            Map<Emotion, Double> emotionWeights,
            int poiLimit
    ) {
        poiLimit = Math.min(poiLimit, MAX_POI_RESULTS);
        poiLimit = Math.max(poiLimit, MIN_POI_RESULTS);
        LOGGER.info("Getting route for conversationId: {}, lat: {}, lon: {}, radius: {}, limit: {}", 
                conversationId, lat, lon, radiusMeters, poiLimit);

        return buildRoute(userId, lat, lon, poiCategories, radiusMeters, emotionWeights, poiLimit)
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
            Map<Emotion, Double> emotionWeights,
            int poiLimit
    ) {
        // Frontend-selected categories are intentionally ignored by the Overpass adapter for now;
        // keep plumbing intact so we can re-enable client-side filtering later.
        Flux<Poi> poiFlux = osmPort.findAmenitiesAround(lat, lon, poiCategories, radiusMeters)
                .cache();

        return poiScoringService.scoreAndRank(poiFlux, userId, emotionWeights, lat, lon, poiLimit)
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

                                // Reorder enrichedPois to start with the one closest to the user's survey coordinate
                                List<EnrichedPoi> reorderedInputPois = new java.util.ArrayList<>(enrichedPois);
                                reorderedInputPois.sort((p1, p2) -> {
                                    double dist1 = calculateHaversineDistance(lat, lon, p1.poi().latitude(), p1.poi().longitude());
                                    double dist2 = calculateHaversineDistance(lat, lon, p2.poi().latitude(), p2.poi().longitude());
                                    return Double.compare(dist1, dist2);
                                });

                                // Move the closest POI to the first position, keep others.
                                // OSRM 'source=first' will force the route to start at this closest POI.

                                
                                return routingPort.calculateRoute(PoiRouteCoordinatesMapper.toCoordinates(
                                                reorderedInputPois.stream().map(EnrichedPoi::poi).toList()
                                        ))
                                        .map(route -> {

                                            double travelTimeSeconds = route.distanceMeters() / WALKING_SPEED_MPS;
                                            double totalDuration = travelTimeSeconds + (enrichedPois.size() * POI_VISITING_TIME_SECONDS);

                                            // Reorder POIs based on the optimized route order from OSRM
                                            // waypointOrder refers to indices in reorderedInputPois (which was sent to OSRM)
                                            List<EnrichedPoi> orderedPOIs = route.waypointOrder().stream()
                                                    .filter(index -> index >= 0 && index < reorderedInputPois.size())
                                                    .map(reorderedInputPois::get)
                                                    .toList();

                                            return new PoiRouteResult(orderedPOIs, new Route(
                                                    route.distanceMeters(), 
                                                    totalDuration, 
                                                    route.geometry(),
                                                    route.legDistances(),
                                                    route.waypointOrder()
                                            ));
                                        });
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
                            })
                            .onErrorResume(error -> {
                                LOGGER.warn("Failed to enrich POI {} (id={}): {}", poi.tags(), poi.osmId(), error.toString());
                                String displayName = PoiDescriptionBuilder.buildDisplayName(poi);
                                String description = PoiDescriptionBuilder.buildShortDescription(poi, "");
                                return Mono.just(new EnrichedPoi(
                                        poi,
                                        displayName,
                                        description,
                                        "",
                                        poi.category(),
                                        scoredPoi.score()
                                ));
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
        if (hasCause(error, MapProviderUnavailableException.class)) {
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

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // convert to meters
    }
}
