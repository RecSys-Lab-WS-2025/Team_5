package de.tum.moodtrip_backend.adapter.maps.osm.mapper;

import de.tum.moodtrip_backend.core.model.PoiCategory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministically maps OSM tags to a single {@link PoiCategory}.
 * <p>
 * Priority order (first match wins):
 * FOOD_AND_CULINARY → SHOPPING → HISTORY_AND_CULTURE → ADVENTURE → RELAXATION → NATURE.
 */
public final class OsmTagCategoryMapper {

    private static final List<PoiCategory> PRIORITY = List.of(
            PoiCategory.FOOD_AND_CULINARY,
            PoiCategory.SHOPPING,
            PoiCategory.HISTORY_AND_CULTURE,
            PoiCategory.ADVENTURE,
            PoiCategory.RELAXATION,
            PoiCategory.NATURE
    );

    private static final Set<String> FOOD_AMENITIES = Set.of(
            "restaurant", "cafe", "bar", "fast_food", "biergarten"
    );

    private static final Set<String> SHOP_TAGS = Set.of(
            "supermarket", "department_store", "mall", "clothes", "shoes", "jewelry",
            "bakery", "butcher", "convenience", "chemist", "books", "gift", "florist"
    );

    private static final Set<String> HISTORY_HISTORIC = Set.of(
            "monument", "memorial", "castle", "ruins", "archaeological_site", "wayside_cross", "wayside_shrine"
    );
    private static final Set<String> HISTORY_TOURISM = Set.of("museum", "gallery", "attraction", "artwork");
    private static final Set<String> HISTORY_AMENITY = Set.of("theatre", "arts_centre", "cinema", "library", "place_of_worship");

    private static final Set<String> ADVENTURE_LEISURE = Set.of(
            "sports_centre", "climbing", "water_park", "golf_course", "pitch", "swimming_pool", "fitness_centre", "stadium", "track"
    );

    private static final Set<String> RELAXATION_LEISURE = Set.of(
            "park", "garden", "nature_reserve", "common", "recreation_ground"
    );

    private static final Set<String> NATURE_VALUES = Set.of(
            "wood", "forest", "wetland", "heath", "scrub", "grassland", "meadow", "water", "lake", "fell", "moor", "peak", "valley", "hill"
    );

    private OsmTagCategoryMapper() {
    }

    public static Optional<PoiCategory> map(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Optional.empty();
        }

        for (PoiCategory category : PRIORITY) {
            if (matches(category, tags)) {
                return Optional.of(category);
            }
        }

        return Optional.empty();
    }

    private static boolean matches(PoiCategory category, Map<String, String> tags) {
        String amenity = tags.getOrDefault("amenity", "");
        String tourism = tags.getOrDefault("tourism", "");
        String leisure = tags.getOrDefault("leisure", "");
        String historic = tags.getOrDefault("historic", "");
        String shop = tags.getOrDefault("shop", "");
        String natural = tags.getOrDefault("natural", "");

        return switch (category) {
            case FOOD_AND_CULINARY -> !amenity.isBlank() && FOOD_AMENITIES.contains(amenity);
            case SHOPPING -> (!shop.isBlank() && SHOP_TAGS.contains(shop)) || "marketplace".equals(shop) || "marketplace".equals(amenity);
            case HISTORY_AND_CULTURE -> (!historic.isBlank() && HISTORY_HISTORIC.contains(historic))
                    || (!tourism.isBlank() && HISTORY_TOURISM.contains(tourism))
                    || (!amenity.isBlank() && HISTORY_AMENITY.contains(amenity));
            case ADVENTURE -> (!leisure.isBlank() && ADVENTURE_LEISURE.contains(leisure))
                    || tags.containsKey("sport")
                    || "zoo".equals(tourism);
            case RELAXATION -> (!leisure.isBlank() && RELAXATION_LEISURE.contains(leisure))
                    || "viewpoint".equals(tourism);
            case NATURE -> !natural.isBlank() && NATURE_VALUES.contains(natural);
        };
    }
}
