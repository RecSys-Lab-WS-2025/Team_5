package de.tum.moodtrip_backend.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import de.tum.moodtrip_backend.adapter_osm.builder.PoiDescriptionBuilder;
import de.tum.moodtrip_backend.core.model.POI;
import de.tum.moodtrip_backend.core.model.POICategory;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.port.OsmPort;
import de.tum.moodtrip_backend.core.port.WikipediaPort;

@Service
public class OverpassService {

    public static final Logger LOGGER = LoggerFactory.getLogger(OverpassService.class);
    private final OsmPort osmPort;
    private final WikipediaPort wikipediaPort;

    public OverpassService(OsmPort osmPort, WikipediaPort wikipediaPort) {
        this.osmPort = osmPort;
        this.wikipediaPort = wikipediaPort;
    }

    public Mono<Route> getRoute(double lat, double lon, String routeType, long radiusMeters) {
        return osmPort.findAmenitiesAround(lat, lon, routeType, radiusMeters).flatMap(osmPort::fetchRelationWithWays);
    }

    public Flux<POI> getPois(double lat, double lon, POICategory poiCategory, int radiusMeters) {
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
                                .thenReturn(poi));
    }
}