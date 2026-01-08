package de.tum.moodtrip_backend.core.model;

import java.util.List;

public record Route(
        double distanceMeters,
        double durationSeconds,
        List<RouteCoordinate> geometry,
        List<Double> legDistances,
        List<Integer> waypointOrder
) {
}