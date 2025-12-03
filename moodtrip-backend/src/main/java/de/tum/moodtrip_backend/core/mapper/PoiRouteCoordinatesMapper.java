package de.tum.moodtrip_backend.core.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.RouteCoordinate;
/**
 * Utility mapper to convert POIs into route coordinates.
 */
public final class PoiRouteCoordinatesMapper {

    private PoiRouteCoordinatesMapper() {
        // utility class
    }

    /**
     * Convert a list of POIs to a list of {@link RouteCoordinate} using their latitude/longitude.
     */
    public static List<RouteCoordinate> toCoordinates(List<Poi> pois) {
        if (pois == null || pois.isEmpty()) {
            return Collections.emptyList();
        }
        List<RouteCoordinate> coordinates = new ArrayList<>(pois.size());
        for (Poi poi : pois) {
            coordinates.add(new RouteCoordinate(poi.latitude(), poi.longitude()));
        }
        return coordinates;
    }

    /**
     * Convert an origin point plus a list of POIs into a list of {@link RouteCoordinate},
     * where the origin is the first element and each POI follows.
     */
    public static List<RouteCoordinate> toCoordinatesWithOrigin(double originLat, double originLon, List<Poi> pois) {
        List<RouteCoordinate> coordinates = new ArrayList<>();
        coordinates.add(new RouteCoordinate(originLat, originLon));
        if (pois != null) {
            for (Poi poi : pois) {
                coordinates.add(new RouteCoordinate(poi.latitude(), poi.longitude()));
            }
        }
        return coordinates;
    }
}
