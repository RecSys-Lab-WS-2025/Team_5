package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.mapper.PoiRouteCoordinatesMapper;
import de.tum.moodtrip_backend.core.mapper.PoiRouteResultRouteRecommendationMapper;
import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.PoiRouteResult;
import de.tum.moodtrip_backend.core.port.RouteRecommendationPort;
import de.tum.moodtrip_backend.core.port.RoutingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.adapter_osm.builder.PoiDescriptionBuilder;
import de.tum.moodtrip_backend.core.model.POICategory;
import de.tum.moodtrip_backend.core.port.OsmPort;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import reactor.core.publisher.Mono;

@Service
public class OverpassService {

    public static final Logger LOGGER = LoggerFactory.getLogger(OverpassService.class);
    private final OsmPort osmPort;
    private final WikipediaPort wikipediaPort;
    private final RoutingPort routingPort;
    private final RouteRecommendationPort routeRecommendationPort;

    public OverpassService(OsmPort osmPort, WikipediaPort wikipediaPort, RoutingPort routingPort, RouteRecommendationPort routeRecommendationPort) {
        this.osmPort = osmPort;
        this.wikipediaPort = wikipediaPort;
        this.routingPort = routingPort;
        this.routeRecommendationPort = routeRecommendationPort;
    }

    public Mono<PoiRouteResult> getRoute(
            long conversationId,
            double lat,
            double lon,
            POICategory poiCategory,
            int radiusMeters
    ) {
        return osmPort.findAmenitiesAround(lat, lon, poiCategory, radiusMeters)
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
                                .doOnNext(route -> LOGGER.info(route.toString()))
                                .map(route -> new PoiRouteResult(enrichedPois, route))
                )
                .flatMap(route ->
                    routeRecommendationPort.save(PoiRouteResultRouteRecommendationMapper.toDomain(route, conversationId)).thenReturn(route)
                );
    }
}