package de.tum.moodtrip_backend.adapter.maps.osrm.mapper;

import java.util.List;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import de.tum.moodtrip_backend.adapter.maps.osrm.model.OsrmRouteResponse;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.model.RouteCoordinate;
@Component
public class OsrmRouteResponseRouteMapper {
    /**
     * Map the OSRM response into your core Route model.
     * Here we take the first route only.
     */
    public Mono<Route> mapToDomainRoute(OsrmRouteResponse osrm) {
        if (osrm.routes() == null || osrm.routes().isEmpty()) {
            return Mono.error(new IllegalStateException("OSRM returned no routes"));
        }
        OsrmRouteResponse.Route osrmRoute = osrm.routes().get(0);

        // Geometry in OSRM: [ [lon, lat], ... ]
        List<RouteCoordinate> geometry = osrmRoute.geometry().coordinates().stream()
                .map(coord -> new RouteCoordinate(
                        coord.get(1), // lat
                        coord.getFirst()  // lon
                ))
                .toList();

        List<Double> legDistances = osrmRoute.legs().stream()
                .map(OsrmRouteResponse.Leg::distance)
                .toList();

        List<Double> legDurations = osrmRoute.legs().stream()
                .map(OsrmRouteResponse.Leg::duration)
                .toList();

        // OSRM /trip waypoints array is in input order.
        // waypoint_index is the position of that input point in the optimized trip.
        // We sort the original indices by their assigned position to get the visitation sequence.
        List<Integer> waypointOrder = java.util.stream.IntStream.range(0, osrm.waypoints().size())
                .boxed()
                .sorted(java.util.Comparator.comparingInt(i -> osrm.waypoints().get(i).waypointIndex()))
                .toList();

        // IMPORTANT: OSRM demo server (router.project-osrm.org) only supports driving profile
        // Even though we use /walking/ endpoint, it returns driving duration
        // So we calculate walking time manually: distance / walking_speed
        // Walking speed: 4.5 km/h = 1.25 m/s
        final double WALKING_SPEED_MPS = 1.25; // meters per second (4.5 km/h)
        
        // Calculate walking duration from total distance
        double calculatedWalkingDuration = osrmRoute.distance() / WALKING_SPEED_MPS;
        
        // Recalculate leg durations proportionally
        // This preserves the relative time distribution between legs
        double originalTotalDuration = osrmRoute.duration();
        List<Double> calculatedLegDurations = legDurations.stream()
                .map(originalLegDuration -> {
                    if (originalTotalDuration > 0) {
                        // Scale each leg duration proportionally
                        return (originalLegDuration / originalTotalDuration) * calculatedWalkingDuration;
                    }
                    return 0.0;
                })
                .toList();

        Route route = new Route(
                osrmRoute.distance(),
                calculatedWalkingDuration, // Use calculated walking duration instead of driving duration
                geometry,
                legDistances,
                calculatedLegDurations, // Use recalculated leg durations
                waypointOrder,
                null, // title - will be set later by route description service
                null  // description - will be set later by route description service
        );

        return Mono.just(route);
    }
}