package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.mapper.PoiRouteCoordinatesMapper;
import de.tum.moodtrip_backend.core.port.RoutingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.tum.moodtrip_backend.adapter_osm.builder.PoiDescriptionBuilder;
import de.tum.moodtrip_backend.core.model.POI;
import de.tum.moodtrip_backend.core.model.POICategory;
import de.tum.moodtrip_backend.core.port.OsmPort;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class OverpassService {

    public static final Logger LOGGER = LoggerFactory.getLogger(OverpassService.class);
    private final OsmPort osmPort;
    private final WikipediaPort wikipediaPort;
    private final RoutingPort routingPort;

    public OverpassService(OsmPort osmPort, WikipediaPort wikipediaPort, RoutingPort routingPort) {
        this.osmPort = osmPort;
        this.wikipediaPort = wikipediaPort;
        this.routingPort = routingPort;
    }

    public Mono<List<POI>> getPois(double lat, double lon, POICategory poiCategory, int radiusMeters) {
        return osmPort.findAmenitiesAround(lat, lon, poiCategory, radiusMeters)
                .flatMap(poi ->
                        wikipediaPort.fetchSummaryForTag(poi.tags().get("wikipedia"))
                                .defaultIfEmpty("")
                                .doOnNext(summary -> {
                                    LOGGER.info(PoiDescriptionBuilder.buildDisplayName(poi));
                                    LOGGER.info(PoiDescriptionBuilder.buildShortDescription(poi, summary));
                                })
                                .then(wikipediaPort.fetchImageUrl(
                                                        poi.tags().get("image"),
                                                        poi.tags().get("wikipedia"),
                                                        poi.tags().get("wikidata"),
                                                        poi.tags().get("wikimedia_commons")
                                                )
                                                .doOnNext(imageUrl ->
                                                        LOGGER.info("Image URL for {}: {}", PoiDescriptionBuilder.buildDisplayName(poi), imageUrl)
                                                )
                                )

                                .thenReturn(poi))
                .collectList()
                .flatMap(list -> routingPort.calculateRoute(PoiRouteCoordinatesMapper.toCoordinates(list))
                        .doOnNext(route -> LOGGER.info(route.toString()))
                        .thenReturn(list));
    }
}