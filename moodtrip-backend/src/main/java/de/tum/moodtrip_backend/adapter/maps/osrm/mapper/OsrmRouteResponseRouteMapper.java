package de.tum.moodtrip_backend.adapter.maps.osrm.mapper;

import de.tum.moodtrip_backend.adapter.maps.osrm.model.OsrmRouteResponse;
import de.tum.moodtrip_backend.core.eval.EvalRun;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.model.RouteCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class OsrmRouteResponseRouteMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsrmRouteResponseRouteMapper.class);

    public Mono<Route> mapToDomainRoute(OsrmRouteResponse osrm) {
        return Mono.deferContextual(ctxView -> {
            EvalRun run = ctxView.hasKey(EvalRun.CTX_KEY) ? ctxView.get(EvalRun.CTX_KEY) : null;
            String routeType = ctxView.hasKey(EvalRun.CTX_ROUTE_TYPE_KEY) ? ctxView.get(EvalRun.CTX_ROUTE_TYPE_KEY) : "UNKNOWN";

            if (osrm.routes() == null || osrm.routes().isEmpty()) {
                return Mono.error(new IllegalStateException("OSRM returned no routes"));
            }
            OsrmRouteResponse.Route osrmRoute = osrm.routes().get(0);

            List<RouteCoordinate> geometry = osrmRoute.geometry().coordinates().stream()
                    .map(coord -> new RouteCoordinate(
                            coord.get(1),     // lat
                            coord.getFirst()  // lon
                    ))
                    .toList();

            List<Double> legDistances = osrmRoute.legs().stream()
                    .map(OsrmRouteResponse.Leg::distance)
                    .toList();

            List<Double> legDurations = osrmRoute.legs().stream()
                    .map(OsrmRouteResponse.Leg::duration)
                    .toList();

            List<Integer> waypointOrder = java.util.stream.IntStream.range(0, osrm.waypoints().size())
                    .boxed()
                    .sorted(java.util.Comparator.comparingInt(i -> osrm.waypoints().get(i).waypointIndex()))
                    .toList();

            // demo server walking durations workaround
            final double WALKING_SPEED_MPS = 1.25;
            double calculatedWalkingDuration = osrmRoute.distance() / WALKING_SPEED_MPS;

            double originalTotalDuration = osrmRoute.duration();
            List<Double> calculatedLegDurations = legDurations.stream()
                    .map(originalLegDuration -> originalTotalDuration > 0
                            ? (originalLegDuration / originalTotalDuration) * calculatedWalkingDuration
                            : 0.0)
                    .toList();

            // EQ1: inter-POI (leg) distance stats
            double meanInterPoiDistM = legDistances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double maxInterPoiDistM = legDistances.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

            LOGGER.info("EVAL_OSRM_ROUTE routeType={} totalDistanceM={} meanInterPoiDistM={} maxInterPoiDistM={} geometryPoints={}",
                    routeType, osrmRoute.distance(), meanInterPoiDistM, maxInterPoiDistM, geometry.size());

            if (run != null) {
                run.markOsrmRouteStats(routeType, meanInterPoiDistM, maxInterPoiDistM, geometry.size());
            }

            Route route = new Route(
                    osrmRoute.distance(),
                    calculatedWalkingDuration,
                    geometry,
                    legDistances,
                    calculatedLegDurations,
                    waypointOrder,
                    null,
                    null
            );

            return Mono.just(route);
        });
    }
}
