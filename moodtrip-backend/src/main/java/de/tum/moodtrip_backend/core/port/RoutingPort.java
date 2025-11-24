package de.tum.moodtrip_backend.core.port;

import java.util.List;

import reactor.core.publisher.Mono;

import de.tum.moodtrip_backend.core.model.RouteCoordinate;
import de.tum.moodtrip_backend.core.model.Route;
public interface RoutingPort {

    /**
     * Calculate a route through the given waypoints (in order).
     *
     * @param waypoints ordered list of waypoints (at least 2)
     * @return a Mono emitting the computed route
     */
    Mono<Route> calculateRoute(List<RouteCoordinate> waypoints);
}