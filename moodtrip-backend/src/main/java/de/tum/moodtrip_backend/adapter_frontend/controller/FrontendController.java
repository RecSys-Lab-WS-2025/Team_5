package de.tum.moodtrip_backend.adapter_frontend.controller;

import de.tum.moodtrip_backend.adapter_frontend.mapper.GeoJsonRouteMapper;
import de.tum.moodtrip_backend.core.model.POICategory;
import de.tum.moodtrip_backend.core.service.OverpassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.geojson.FeatureCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/frontend", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Route Recommendations", description = "APIs for generating location-based route recommendations")
public class FrontendController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendController.class);

    private final OverpassService overpassService;

    public FrontendController(OverpassService overpassService) {
        this.overpassService = overpassService;
    }

    @Operation(
        summary = "Get recommended route with POIs",
        description = "Generates a GeoJSON route with points of interest based on user location, category preferences, and search radius"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Route generated successfully with POI recommendations"
        )
    })
    @GetMapping(value = "/route", produces = "application/geo+json")
    public Mono<FeatureCollection> getRoute(
            @Parameter(description = "Conversation ID", required = true) @RequestParam("conversationId") long conversationId,
            @Parameter(description = "Latitude coordinate", required = true) @RequestParam("lat") double lat,
            @Parameter(description = "Longitude coordinate", required = true) @RequestParam("lon") double lon,
            @Parameter(description = "POI category for recommendations", required = true) @RequestParam("poiCategory") String poiCategory,
            @Parameter(description = "Search radius in meters", required = true) @RequestParam("radiusMeters") int radiusMeters) {

        LOGGER.info("GET /frontend/route lat={}, lon={}, poiCategory={}, radius={}",
                lat, lon, poiCategory, radiusMeters);

        POICategory category = POICategory.fromDisplayName(poiCategory);
        return overpassService
                .getRoute(conversationId, lat, lon, category, radiusMeters)
                .map(GeoJsonRouteMapper::toFeatureCollection);
    }
}
