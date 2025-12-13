package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.mapper.PoiRouteCoordinatesMapper;
import de.tum.moodtrip_backend.core.mapper.PoiRouteResultRouteRecommendationMapper;
import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.PoiRouteResult;
import de.tum.moodtrip_backend.core.model.RouteGenerationResult;
import de.tum.moodtrip_backend.core.port.RouteRecommendationPort;
import de.tum.moodtrip_backend.core.port.RoutingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.adapter.maps.osm.builder.PoiDescriptionBuilder;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.port.OsmPort;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
public class RouteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteService.class);
    private static final Duration ROUTE_TIMEOUT = Duration.ofSeconds(30);
    private static final String GENERIC_ERROR_MESSAGE = "I couldn't generate a route due to a routing service error. Please try again.";

    private final OsmPort osmPort;
    private final WikipediaPort wikipediaPort;
    private final RoutingPort routingPort;
    private final RouteRecommendationPort routeRecommendationPort;

    public RouteService(OsmPort osmPort, WikipediaPort wikipediaPort, RoutingPort routingPort, RouteRecommendationPort routeRecommendationPort) {
        this.osmPort = osmPort;
        this.wikipediaPort = wikipediaPort;
        this.routingPort = routingPort;
        this.routeRecommendationPort = routeRecommendationPort;
    }

    public Mono<RouteGenerationResult> getRoute(
            long conversationId,
            double lat,
            double lon,
            List<PoiCategory> poiCategories,
            int radiusMeters
    ) {
        LOGGER.info("Getting route for conversationId: {}, lat: {}, lon: {}, radius: {}", conversationId, lat, lon, radiusMeters);

        return buildRoute(lat, lon, poiCategories, radiusMeters)
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
            double lat,
            double lon,
            List<PoiCategory> poiCategories,
            int radiusMeters
    ) {
        return osmPort.findAmenitiesAround(lat, lon, poiCategories, radiusMeters)
                .flatMap(poi ->
                        wikipediaPort.fetchSummaryForTag(poi.tags().get("wikipedia"))
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

                                    return new EnrichedPoi(poi, displayName, description, imageUrl);
                                })
                )
                .collectList()
                .flatMap(enrichedPois ->
                        routingPort.calculateRoute(PoiRouteCoordinatesMapper.toCoordinates(
                                        enrichedPois.stream().map(EnrichedPoi::poi).toList()
                                ))
                                .map(route -> new PoiRouteResult(enrichedPois, route))
                );
    }

    private String mapErrorToUserMessage(Throwable error) {
        if (error == null) {
            return GENERIC_ERROR_MESSAGE;
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
}
