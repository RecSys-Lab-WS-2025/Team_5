package de.tum.moodtrip_backend.core.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.core.exception.MapProviderUnavailableException;
import de.tum.moodtrip_backend.core.mapper.PoiRouteCoordinatesMapper;
import de.tum.moodtrip_backend.core.mapper.PoiRouteResultRouteRecommendationMapper;
import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiRouteResult;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.model.RouteGenerationResult;
import de.tum.moodtrip_backend.core.model.RouteText;
import de.tum.moodtrip_backend.core.model.RouteGenerationContext;
import de.tum.moodtrip_backend.core.model.ScoredPoi;
import de.tum.moodtrip_backend.core.model.ScoringConfig;
import de.tum.moodtrip_backend.core.port.OsmPort;
import de.tum.moodtrip_backend.core.port.RouteRecommendationPort;
import de.tum.moodtrip_backend.core.port.RoutingPort;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import de.tum.moodtrip_backend.core.util.PoiDescriptionBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RouteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteService.class);
    private static final Duration ROUTE_TIMEOUT = Duration.ofSeconds(90);
    private static final String GENERIC_ERROR_MESSAGE = "I couldn't generate a route due to a routing service error. Please try again.";
    private static final int MAX_POI_RESULTS = 20;
    private static final int MIN_POI_RESULTS = 2;
    public static final int POI_VISITING_TIME_SECONDS = 45 * 60;
    public static final double WALKING_SPEED_MPS = 5000.0 / 3600.0;

    private final OsmPort osmPort;
    private final WikipediaPort wikipediaPort;
    private final RoutingPort routingPort;
    private final RouteRecommendationPort routeRecommendationPort;
    private final PoiScoringService poiScoringService;
    private final RouteDescriptionService routeDescriptionService;
    private final ScoringConfigFactory scoringConfigFactory;

    public RouteService(OsmPort osmPort,
                        WikipediaPort wikipediaPort,
                        RoutingPort routingPort,
                        RouteRecommendationPort routeRecommendationPort,
                        PoiScoringService poiScoringService,
                        RouteDescriptionService routeDescriptionService,
                        ScoringConfigFactory scoringConfigFactory) {
        this.osmPort = osmPort;
        this.wikipediaPort = wikipediaPort;
        this.routingPort = routingPort;
        this.routeRecommendationPort = routeRecommendationPort;
        this.poiScoringService = poiScoringService;
        this.routeDescriptionService = routeDescriptionService;
        this.scoringConfigFactory = scoringConfigFactory;
    }



    /**
     * Generates multiple routes with different scoring strategies.
     * Returns 3 routes: emotion-focused, category-focused, and balanced.
     */
    public Mono<List<RouteGenerationResult>> getMultipleRoutes(
            long conversationId,
            long userId,
            double lat,
            double lon,
            List<PoiCategory> poiCategories,
            int radiusMeters,
            Map<Emotion, Double> emotionWeights,
            int poiLimit,
            String city,
            int tripDays,
            boolean isMocked
    ) {
        int normalizedPoiLimit = Math.min(Math.max(poiLimit, MIN_POI_RESULTS), MAX_POI_RESULTS);
        LOGGER.info("[START] Generating 3 routes for conversationId: {}; city: {}; poiLimit: {}; lat/lon: {},{}",
                conversationId, city, normalizedPoiLimit, lat, lon);

        // Fetch POIs once and cache for all 3 routes
        Flux<Poi> cachedPoiFlux = osmPort.findAmenitiesAround(lat, lon, poiCategories, radiusMeters)
                .cache();

        // Get all scoring configurations from properties
        ScoringConfig[] configs = scoringConfigFactory.allConfigs();

        String mood = getTopEmotion(emotionWeights);
        String cityName = (city != null && !city.trim().isEmpty()) ? city.trim() : "Unknown City";

        // 1. Generate geometry-only routes in parallel
        return Flux.fromArray(configs)
                .flatMap(config -> buildRouteWithConfig(userId, lat, lon, poiCategories, radiusMeters, emotionWeights, normalizedPoiLimit, cachedPoiFlux, config)
                        .timeout(ROUTE_TIMEOUT)
                        .map(route -> new ConfiguredRoute(config, route))
                        .onErrorResume(ex -> {
                            LOGGER.error("Error generating {} route for conversationId: {}", config.routeType(), conversationId, ex);
                            // We return empty so we can continue with other routes
                            return Mono.empty();
                        })
                )
                .collectList()
                .flatMap(configuredRoutes -> {
                    if (configuredRoutes.isEmpty()) {
                        return Mono.just(List.of(RouteGenerationResult.failure("Failed to generate any routes.")));
                    }

                    // 2. Prepare context for batch AI generation
                    List<RouteGenerationContext> contexts = configuredRoutes.stream()
                            .map(cr -> new RouteGenerationContext(
                                    cr.config.routeType(),
                                    cr.route.pois(),
                                    tripDays
                            ))
                            .toList();

                    // 3. Call batch generation service
                    return routeDescriptionService.generateBatchRouteText(mood, cityName, contexts, isMocked)
                            .flatMapMany(descriptions -> {
                                // 4. Map descriptions back to routes and save
                                return Flux.fromIterable(configuredRoutes)
                                        .flatMap(configuredRoute -> {
                                            RouteText text = descriptions.get(configuredRoute.config.routeType());
                                            // Fallback to default if text is missing
                                            if (text == null) {
                                                text = new RouteText("Route", Map.of());
                                            }

                                            Route originalRoute = configuredRoute.route.route();
                                            Route routeWithTitleAndDesc = new Route(
                                                    originalRoute.distanceMeters(),
                                                    originalRoute.durationSeconds(),
                                                    originalRoute.geometry(),
                                                    originalRoute.legDistances(),
                                                    originalRoute.legDurations(),
                                                    originalRoute.waypointOrder(),
                                                    text.title(),
                                                    text.dayDescriptions()
                                            );
                                            PoiRouteResult finalResult = new PoiRouteResult(configuredRoute.route.pois(), routeWithTitleAndDesc);

                                            return routeRecommendationPort.save(PoiRouteResultRouteRecommendationMapper.toDomain(finalResult, conversationId))
                                                    .doOnNext(saved -> LOGGER.info("[STEP] Route ({}) generated and saved successfully for conversationId: {}", configuredRoute.config.routeType(), saved.conversationId()))
                                                    .map(saved -> RouteGenerationResult.success(finalResult, configuredRoute.config.routeType()));
                                        });
                            })
                            .collectList();
                })
                .map(results -> {
                    // Sort results to maintain consistent order: EMOTION_FOCUSED, CATEGORY_FOCUSED, BALANCED
                    results.sort((a, b) -> {
                        if (a.routeType() == null && b.routeType() == null) return 0;
                        if (a.routeType() == null) return 1;
                        if (b.routeType() == null) return -1;
                        return a.routeType().ordinal() - b.routeType().ordinal();
                    });
                    return results;
                })
                .doOnNext(results -> LOGGER.info("[COMPLETE] Batch generation finished for conversationId: {}. Generated {} routes.", conversationId, results.size()));
    }
    
    private String getTopEmotion(Map<Emotion, Double> emotionWeights) {
        return emotionWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().name())
                .orElse("NEUTRAL");
    }


    private record ConfiguredRoute(ScoringConfig config, PoiRouteResult route) {}

    private Mono<PoiRouteResult> buildRouteWithConfig(
            long userId,
            double lat,
            double lon,
            List<PoiCategory> poiCategories,
            int radiusMeters,
            Map<Emotion, Double> emotionWeights,
            int poiLimit,
            Flux<Poi> cachedPoiFlux,
            ScoringConfig config
    ) {
        return poiScoringService.scoreAndRank(cachedPoiFlux, userId, emotionWeights, poiCategories, lat, lon, poiLimit, config)
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

                                List<EnrichedPoi> reorderedInputPois = new java.util.ArrayList<>(enrichedPois);
                                reorderedInputPois.sort((p1, p2) -> {
                                    double dist1 = calculateHaversineDistance(lat, lon, p1.poi().latitude(), p1.poi().longitude());
                                    double dist2 = calculateHaversineDistance(lat, lon, p2.poi().latitude(), p2.poi().longitude());
                                    return Double.compare(dist1, dist2);
                                });

                                return routingPort.calculateRoute(PoiRouteCoordinatesMapper.toCoordinates(
                                                reorderedInputPois.stream().map(EnrichedPoi::poi).toList()
                                        ))
                                        .map(route -> {
                                            double totalDuration = route.durationSeconds() + (enrichedPois.size() * POI_VISITING_TIME_SECONDS);
                                            List<EnrichedPoi> orderedPOIs = route.waypointOrder().stream()
                                                    .filter(index -> index >= 0 && index < reorderedInputPois.size())
                                                    .map(reorderedInputPois::get)
                                                    .toList();

                                            return new PoiRouteResult(orderedPOIs, new Route(
                                                    route.distanceMeters(),
                                                    totalDuration,
                                                    route.geometry(),
                                                    route.legDistances(),
                                                    route.legDurations(),
                                                    route.waypointOrder(),
                                                    null,
                                                    null
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

    //helper method to calculate nearest POI to given coordinates
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000;
    }
}