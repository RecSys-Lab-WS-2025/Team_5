package de.tum.moodtrip_backend.adapter_frontend.controller;

import de.tum.moodtrip_backend.adapter_frontend.mapper.RouteGeoJsonMapper;
import de.tum.moodtrip_backend.core.model.POI;
import de.tum.moodtrip_backend.core.model.POICategory;
import de.tum.moodtrip_backend.core.service.OverpassService;
import org.geojson.FeatureCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/frontend", produces = MediaType.APPLICATION_JSON_VALUE)
public class FrontendController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendController.class);

    private final OverpassService overpassService;
    private final RouteGeoJsonMapper routeGeoJsonMapper;

    public FrontendController(OverpassService overpassService, RouteGeoJsonMapper routeGeoJsonMapper) {
        this.overpassService = overpassService;
        this.routeGeoJsonMapper = routeGeoJsonMapper;
    }

    @GetMapping(value = "/route", produces = "application/geo+json")
    public Mono<FeatureCollection> getRoute(@RequestParam("lat") double lat,
                                            @RequestParam("lon") double lon,
                                            @RequestParam("routeType") String routeType,
                                            @RequestParam("radiusMeters") long radiusMeters) {
        LOGGER.info("GET /frontend/route lat={}, lon={}, routeType={}, radiusMeters={}", lat, lon, routeType, radiusMeters);
        return overpassService.getRoute(lat, lon, routeType, radiusMeters)
                .map(routeGeoJsonMapper::toFeatureCollection);
    }

    @GetMapping(value="/poi")
    public Flux<POI> getPois(@RequestParam("lat") double lat,
                             @RequestParam("lon") double lon,
                             @RequestParam("poiCategory") String poiCategory,
                             @RequestParam("radiusMeters") int radiusMeters) {

        LOGGER.info("GET /frontend/poi lat={}, lon={}, routeType={}, radius={}",
                lat, lon, poiCategory, radiusMeters);

        POICategory category = POICategory.fromDisplayName(poiCategory)
;
        return overpassService
                .getPois(lat, lon, category, radiusMeters);
    }
}
