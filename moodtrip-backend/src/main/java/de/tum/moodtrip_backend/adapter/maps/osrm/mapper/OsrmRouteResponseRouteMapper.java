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

        Route route = new Route(
                osrmRoute.distance(),
                osrmRoute.duration(),
                geometry
        );

        return Mono.just(route);
    }
}
