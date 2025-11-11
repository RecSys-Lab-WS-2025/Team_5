package de.tum.moodtrip_backend.adapter_osm.model;

import java.util.List;
import java.util.Map;

public record OverpassResponse(
        String version,
        String generator,
        Osm3s osm3s,
        List<Element> elements
) {
    public record Osm3s(String timestamp_osm_base, String copyright) {}

    public record Element(
            String type,
            long id,
            Double lat,
            Double lon,
            Map<String, String> tags,
            List<Point> geometry
    ) {}

    public record Point(double lat, double lon) {}
}