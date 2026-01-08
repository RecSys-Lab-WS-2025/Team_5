package de.tum.moodtrip_backend.adapter.maps.osrm.adapter;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import de.tum.moodtrip_backend.adapter.maps.osrm.mapper.OsrmRouteResponseRouteMapper;
import de.tum.moodtrip_backend.adapter.maps.osrm.model.OsrmRouteResponse;
import de.tum.moodtrip_backend.core.model.RouteCoordinate;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.port.RoutingPort;

@Component
public class OsrmAdapter implements RoutingPort {

    private final WebClient osrmWebClient;
    private final OsrmRouteResponseRouteMapper osrmRouteResponseRouteMapper;

    public OsrmAdapter(WebClient.Builder builder, OsrmRouteResponseRouteMapper osrmRouteResponseRouteMapper) {
        this.osrmWebClient = builder.baseUrl("http://router.project-osrm.org").build();
        this.osrmRouteResponseRouteMapper = osrmRouteResponseRouteMapper;
    }

    @Override
    public Mono<Route> calculateRoute(List<RouteCoordinate> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            return Mono.error(new IllegalArgumentException("At least two waypoints are required"));
        }

        String coordinatesPath = buildCoordinatesPath(waypoints);

        return osrmWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trip/v1/walking/{coords}")
                        .queryParam("geometries", "geojson")
                        .queryParam("source", "first")
                        .queryParam("roundtrip", "false")
                        .build(coordinatesPath))
                .retrieve()
                .bodyToMono(OsrmRouteResponse.class)
                .flatMap(osrmRouteResponseRouteMapper::mapToDomainRoute);
    }

    /**
     * OSRM expects "lon,lat;lon,lat;lon,lat" in the path.
     */
    private String buildCoordinatesPath(List<RouteCoordinate> waypoints) {
        return waypoints.stream()
                .map(p -> p.lon() + "," + p.lat())
                .collect(Collectors.joining(";"));
    }

}