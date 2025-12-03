package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import reactor.core.publisher.Flux;

public interface OsmPort {
    Flux<Poi> findAmenitiesAround(double lat, double lon, PoiCategory poiCategory, int radiusMeters);
}
