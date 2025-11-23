package de.tum.moodtrip_backend.adapter_frontend.controller;

import de.tum.moodtrip_backend.core.model.POI;
import de.tum.moodtrip_backend.core.model.POICategory;
import de.tum.moodtrip_backend.core.service.OverpassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(path = "/frontend", produces = MediaType.APPLICATION_JSON_VALUE)
public class FrontendController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontendController.class);

    private final OverpassService overpassService;

    public FrontendController(OverpassService overpassService) {
        this.overpassService = overpassService;
    }

    @GetMapping(value = "/poi")
    public Flux<POI> getPois(@RequestParam("lat") double lat,
                             @RequestParam("lon") double lon,
                             @RequestParam("poiCategory") String poiCategory,
                             @RequestParam("radiusMeters") int radiusMeters) {

        LOGGER.info("GET /frontend/poi lat={}, lon={}, poiCategory={}, radius={}",
                lat, lon, poiCategory, radiusMeters);

        POICategory category = POICategory.fromDisplayName(poiCategory);
        return overpassService
                .getPois(lat, lon, category, radiusMeters);
    }
}
