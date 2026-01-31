package de.tum.moodtrip_backend.adapter.maps.osrm.adapter;

import de.tum.moodtrip_backend.adapter.maps.osrm.mapper.OsrmRouteResponseRouteMapper;
import de.tum.moodtrip_backend.adapter.maps.osrm.model.OsrmRouteResponse;
import de.tum.moodtrip_backend.core.eval.EvalRun;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.model.RouteCoordinate;
import de.tum.moodtrip_backend.core.port.RoutingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OsrmAdapter implements RoutingPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsrmAdapter.class);

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

        return Mono.deferContextual(ctxView -> {
            EvalRun run = ctxView.hasKey(EvalRun.CTX_KEY) ? ctxView.get(EvalRun.CTX_KEY) : null;
            String routeType = ctxView.hasKey(EvalRun.CTX_ROUTE_TYPE_KEY) ? ctxView.get(EvalRun.CTX_ROUTE_TYPE_KEY) : "UNKNOWN";

            long t0 = System.nanoTime();

            return osrmWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/trip/v1/walking/{coords}")
                            .queryParam("geometries", "geojson")
                            .queryParam("source", "first")
                            .queryParam("roundtrip", "false")
                            .build(coordinatesPath))
                    .retrieve()
                    .bodyToMono(OsrmRouteResponse.class)
                    .doOnSuccess(resp -> {
                        long ms = (System.nanoTime() - t0) / 1_000_000;

                        double dist = 0.0;
                        int legs = 0;
                        if (resp != null && resp.routes() != null && !resp.routes().isEmpty()) {
                            var r = resp.routes().get(0);
                            dist = r.distance();
                            legs = (r.legs() != null) ? r.legs().size() : 0;
                        }

                        LOGGER.info("EVAL_OSRM routeType={} latencyMs={} totalDistanceM={} legs={}", routeType, ms, dist, legs);
                        if (run != null) run.markOsrmOk(routeType, ms, dist, legs);
                    })
                    .doOnError(e -> {
                        long ms = (System.nanoTime() - t0) / 1_000_000;
                        LOGGER.warn("EVAL_OSRM_FAIL routeType={} latencyMs={} error={}", routeType, ms, e.toString());
                        if (run != null) run.markOsrmFail(routeType, ms, e.toString());
                    })
                    .flatMap(osrmRouteResponseRouteMapper::mapToDomainRoute);
        });
    }

    private String buildCoordinatesPath(List<RouteCoordinate> waypoints) {
        return waypoints.stream()
                .map(p -> p.lon() + "," + p.lat())
                .collect(Collectors.joining(";"));
    }
}
