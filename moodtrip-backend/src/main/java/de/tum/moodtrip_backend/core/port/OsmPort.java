package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import reactor.core.publisher.Flux;

import java.util.List;

public interface OsmPort {
    Flux<Poi> findAmenitiesAround(double lat, double lon, List<PoiCategory> poiCategory, int radiusMeters);
}
