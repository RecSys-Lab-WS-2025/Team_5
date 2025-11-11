package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.Route;
import reactor.core.publisher.Mono;

public interface OsmPort {
    Mono<Route> fetchRelationWithWays(long relationId);

    Mono<Long> findAmenitiesAround(double lat, double lon, String routeType, long radiusMeters);
}
