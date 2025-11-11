package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.port.OsmPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OverpassService {
    private final OsmPort osmPort;

    public OverpassService(OsmPort osmPort) {
        this.osmPort = osmPort;
    }

    public Mono<Route> getRoute(double lat, double lon, String routeType, long radiusMeters) {
        return osmPort.findAmenitiesAround(lat, lon, routeType, radiusMeters).flatMap(osmPort::fetchRelationWithWays);
    }
}