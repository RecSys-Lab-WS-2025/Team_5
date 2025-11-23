package de.tum.moodtrip_backend.adapter_osm.mapper;

import de.tum.moodtrip_backend.adapter_osm.model.OverpassResponse;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.model.RouteCoordinate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public final class OverpassResponseRouteMapper {

    private OverpassResponseRouteMapper() {
    }

    public static Route toDomain(OverpassResponse overpassResponse, Long relationId) {
        String name = overpassResponse.elements().stream()
                .filter(e -> "relation".equals(e.type()))
                .map(e -> firstNonBlank(
                        e.tags() == null ? null : e.tags().get("name"),
                        e.tags() == null ? null : e.tags().get("int_name"),
                        "route " + relationId))
                .findFirst()
                .orElse("route " + relationId);

        List<List<RouteCoordinate>> lines = new ArrayList<>();

        overpassResponse.elements().stream()
                .filter(e -> "way".equals(e.type()) && e.geometry() != null)
                .sorted(Comparator.comparingLong(OverpassResponse.Element::id))
                .forEach(e -> {
                    var coords = e.geometry().stream()
                            .map(p -> new RouteCoordinate(p.lat(), p.lon()))
                            .toList();
                    if (!coords.isEmpty()) {
                        lines.add(coords);
                    }
                });

        return new Route(relationId, name, lines);
    }

    private static String firstNonBlank(String... cs) {
        for (String c : cs) if (c != null && !c.isBlank()) return c;
        throw new RuntimeException("No way found");
    }
}
