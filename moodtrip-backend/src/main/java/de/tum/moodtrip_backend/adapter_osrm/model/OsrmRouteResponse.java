package de.tum.moodtrip_backend.adapter_osrm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OsrmRouteResponse(
        String code,
        @JsonProperty("trips") List<Route> routes,
        List<Waypoint> waypoints
) {

    public record Route(
            double distance,
            double duration,
            double weight,
            String weight_name,
            Geometry geometry,
            List<Leg> legs
    ) {}

    public record Leg(
            double distance,
            double duration,
            double weight,
            String summary,
            List<Step> steps
    ) {}

    public record Step(
            double distance,
            double duration,
            String name,
            String mode,
            Maneuver maneuver,
            Geometry geometry
    ) {}

    public record Maneuver(
            String type,
            String instruction
    ) {}

    public record Geometry(
            String type,
            List<List<Double>> coordinates // [ [lon, lat], ... ]
    ) {}

    public record Waypoint(
            String name,
            List<Double> location, // [lon, lat]
            double distance,
            String hint
    ) {}
}