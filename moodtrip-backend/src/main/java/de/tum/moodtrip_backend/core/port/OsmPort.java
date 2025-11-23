package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.POI;
import de.tum.moodtrip_backend.core.model.POICategory;
import reactor.core.publisher.Flux;

public interface OsmPort {
    Flux<POI> findAmenitiesAround(double lat, double lon, POICategory poiCategory, int radiusMeters);
}
