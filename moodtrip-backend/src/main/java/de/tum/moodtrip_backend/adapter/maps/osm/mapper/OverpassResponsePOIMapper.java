package de.tum.moodtrip_backend.adapter.maps.osm.mapper;

import de.tum.moodtrip_backend.adapter.maps.osm.model.OverpassResponse;
import de.tum.moodtrip_backend.core.model.POI;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OverpassResponsePOIMapper {

    private OverpassResponsePOIMapper() {
    }

    public static List<POI> toPois(OverpassResponse response) {
        if (response == null || response.elements() == null) {
            return List.of();
        }

        return response.elements().stream()
                .map(OverpassResponsePOIMapper::toPoi)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<POI> toPoi(OverpassResponse.Element element) {
        if (element == null) {
            return Optional.empty();
        }

        Double lat = element.lat();
        Double lon = element.lon();

        if (lat == null || lon == null) {
            if (element.geometry() != null && !element.geometry().isEmpty()) {
                OverpassResponse.Point p = element.geometry().getFirst();
                lat = p.lat();
                lon = p.lon();
            } else {
                return Optional.empty();
            }
        }

        Map<String, String> tags = element.tags() != null
                ? element.tags()
                : Collections.emptyMap();

        String name = tags.get("name");

        POI.OsmType osmType;
        try {
            osmType = POI.OsmType.valueOf(element.type().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }

        POI poi = new POI(
                element.id(),
                osmType,
                lat,
                lon,
                name,
                tags
        );

        return Optional.of(poi);
    }
}