package de.tum.moodtrip_backend.core.model;

import java.util.List;
import java.util.Map;

public record Route(
        double distanceMeters,
        double durationSeconds,
        List<RouteCoordinate> geometry,
        List<Double> legDistances,
        List<Double> legDurations,
        List<Integer> waypointOrder,
        String title,
        Map<Integer, String> dayDescriptions
) {
}