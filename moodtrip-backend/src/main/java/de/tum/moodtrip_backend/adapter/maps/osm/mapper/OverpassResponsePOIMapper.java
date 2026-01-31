package de.tum.moodtrip_backend.adapter.maps.osm.mapper;

import de.tum.moodtrip_backend.adapter.maps.osm.model.OverpassResponse;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class OverpassResponsePOIMapper {

    private OverpassResponsePOIMapper() {}

    // Trust Marker Filter (paper 5.2.2)
    private static final Set<String> TRUST_TAGS = Set.of(
            "name", "wikipedia", "website", "image", "wikidata",
            "contact:website", "contact:wikipedia", "wikimedia_commons"
    );

    public static List<Poi> toPois(OverpassResponse response) {
        if (response == null || response.elements() == null) {
            return List.of();
        }

        return response.elements().stream()
                .map(OverpassResponsePOIMapper::toPoi)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<Poi> toPoi(OverpassResponse.Element element) {
        if (element == null) return Optional.empty();

        Double lat = element.lat();
        Double lon = element.lon();
        if (lat == null || lon == null) return Optional.empty();

        Map<String, String> tags = element.tags() != null ? element.tags() : Collections.emptyMap();

        boolean trustworthy = TRUST_TAGS.stream().anyMatch(tags::containsKey);
        if (!trustworthy) return Optional.empty();

        String name = tags.get("name");

        PoiCategory category = OsmTagCategoryMapper.map(tags).orElse(null);
        if (category == null) return Optional.empty();

        Poi.OsmType osmType;
        try {
            osmType = Poi.OsmType.valueOf(element.type().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }

        Poi poi = new Poi(
                element.id(),
                osmType,
                lat,
                lon,
                name,
                category,
                tags
        );

        return Optional.of(poi);
    }
}
