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
            "restaurant",
            "cafe",
            "biergarten",
            "ice_cream",
            "tea_room",
            "pub"
    );

    private static final Set<String> FOOD_SHOP_TAGS = Set.of(
            "confectionery",
            "cheese",
            "delicatessen"
    );

    private static final Set<String> SHOP_TAGS = Set.of(
            "department_store",
            "mall",
            "antique",
            "art",
            "craft",
            "handicraft",
            "bookstore",
            "music"
    );

    private static final Set<String> HISTORY_HISTORIC = Set.of(
            "monument",
            "memorial",
            "castle",
            "ruins",
            "archaeological_site",
            "fort",
            "heritage_site"
    );

    private static final Set<String> HISTORY_TOURISM = Set.of(
            "museum",
            "gallery",
            "attraction",
            "artwork",
            "heritage_site"
    );

    private static final Set<String> HISTORY_AMENITY = Set.of(
            "theatre",
            "arts_centre",
            "place_of_worship"
    );

    private static final Set<String> ADVENTURE_LEISURE = Set.of(
            "sports_centre",
            "swimming_pool",
            "ice_rink",
            "horse_riding",
            "high_ropes_course",
            "track"
    );

    private static final Set<String> ADVENTURE_TOURISM = Set.of(
            "alpine_hut",
            "camp_site",
            "theme_park"
    );

    private static final Set<String> RELAXATION_LEISURE = Set.of(
            "park",
            "garden",
            "recreation_ground",
            "picnic_site",
            "playground"
    );

    private static final Set<String> RELAXATION_TOURISM = Set.of(
            "picnic_site"
    );

    private static final Set<String> NATURE_VALUES = Set.of(
            "wood",
            "grassland",
            "wetland",
            "water",
            "heath",
            "sand",
            "scrub"
    );

    private static final Set<String> NATURE_LANDUSE = Set.of(
            "forest",
            "meadow"
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
        String building = tags.getOrDefault("building", "");
        String sport = tags.getOrDefault("sport", "");
        String shop = tags.getOrDefault("shop", "");
        String natural = tags.getOrDefault("natural", "");
        String landuse = tags.getOrDefault("landuse", "");
        String boundary = tags.getOrDefault("boundary", "");
        String protectClass = tags.getOrDefault("protect_class", "");

        return switch (category) {
            case FOOD_AND_CULINARY -> (
                    (!amenity.isBlank() && FOOD_AMENITIES.contains(amenity))
                            || (!shop.isBlank() && FOOD_SHOP_TAGS.contains(shop))
            );
            case SHOPPING -> (
                    (!shop.isBlank() && SHOP_TAGS.contains(shop))
                            || "marketplace".equals(amenity)
            );
            case HISTORY_AND_CULTURE -> (!historic.isBlank() && HISTORY_HISTORIC.contains(historic))
                    || (!tourism.isBlank() && HISTORY_TOURISM.contains(tourism))
                    || (!amenity.isBlank() && HISTORY_AMENITY.contains(amenity))
                    || "historic".equals(building);
            case ADVENTURE -> (
                    (!leisure.isBlank() && ADVENTURE_LEISURE.contains(leisure))
                            || (!tourism.isBlank() && ADVENTURE_TOURISM.contains(tourism))
                            || ("track".equals(leisure) && !sport.isBlank())
            );
            case RELAXATION -> (
                    (!leisure.isBlank() && RELAXATION_LEISURE.contains(leisure))
                            || (!tourism.isBlank() && RELAXATION_TOURISM.contains(tourism))
            );
            case NATURE -> (
                    "protected_area".equals(boundary)
                            || !protectClass.isBlank()
                            || "nature_reserve".equals(leisure)
                            || (!natural.isBlank() && NATURE_VALUES.contains(natural))
                            || (!landuse.isBlank() && NATURE_LANDUSE.contains(landuse))
                            || "viewpoint".equals(tourism)
            );
        };
    }
}
