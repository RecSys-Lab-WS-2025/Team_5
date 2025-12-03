package de.tum.moodtrip_backend.adapter.maps.osm.builder;

import de.tum.moodtrip_backend.core.model.PoiCategory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class POICategoryOsmQueryBuilder {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private static final Map<PoiCategory, List<String>> CATEGORY_TO_FRAGMENTS =
            Map.of(
                    PoiCategory.NATURE, List.of(
                            "nwr[\"leisure\"~\"park|garden|nature_reserve\"]",
                            "nwr[\"boundary\"=\"protected_area\"]",
                            "nwr[\"natural\"~\"wood|forest|wetland|heath|scrub|grassland|meadow|water|lake|fell|moor|peak|valley\"]",
                            "nwr[\"tourism\"~\"picnic_site\"]"
                    ),

                    PoiCategory.HISTORY_AND_CULTURE, List.of(
                            "nwr[\"historic\"]",
                            "nwr[\"heritage\"]",
                            "nwr[\"memorial\"]",
                            "nwr[\"tourism\"=\"attraction\"][\"historic\"]",
                            "nwr[\"amenity\"=\"place_of_worship\"]"
                    ),

                    PoiCategory.ADVENTURE, List.of(
                            "nwr[\"leisure\"~\"sports_centre|pitch|stadium|water_park|adventure_park\"]",
                            "nwr[\"tourism\"=\"theme_park\"]",
                            "nwr[\"sport\"~\"climbing|skiing|snowboard|surfing|diving|canoe|kayak|rafting|skateboard|mtb|paragliding\"]"
                    ),

                    PoiCategory.RELAXATION, List.of(
                            "nwr[\"leisure\"~\"park|garden|spa\"]",
                            "nwr[\"amenity\"~\"spa|sauna\"]",
                            "nwr[\"tourism\"~\"hotel|guest_house|resort\"]"
                    ),

                    PoiCategory.FOOD_AND_CULINARY, List.of(
                            "nwr[\"amenity\"~\"restaurant|cafe|fast_food|biergarten|pub|bar|food_court|ice_cream|brewery\"]",
                            "nwr[\"craft\"~\"winery|brewery\"]",
                            "nwr[\"shop\"~\"bakery|confectionery|deli|butcher\"]"
                    ),

                    PoiCategory.NIGHTLIFE, List.of(
                            "nwr[\"amenity\"~\"bar|pub|biergarten|nightclub|casino|music_venue\"]"
                    ),

                    PoiCategory.ART_AND_MUSEUMS, List.of(
                            "nwr[\"tourism\"~\"museum|gallery|artwork\"]",
                            "nwr[\"amenity\"~\"arts_centre|theatre|cinema\"]",
                            "nwr[\"historic\"=\"artwork\"]"
                    ),

                    PoiCategory.SHOPPING, List.of(
                            "nwr[\"shop\"~\"mall|department_store|supermarket|clothes|shoes|electronics|books|gift|boutique\"]",
                            "nwr[\"amenity\"=\"marketplace\"]",
                            "nwr[\"landuse\"=\"retail\"]"
                    ),

                    PoiCategory.BEACH_AND_COAST, List.of(
                            "nwr[\"natural\"~\"beach|coastline|bay|dune\"]",
                            "nwr[\"tourism\"=\"beach_resort\"]",
                            "nwr[\"leisure\"=\"beach_resort\"]",
                            "nwr[\"place\"~\"island|islet\"]"
                    ),

                    PoiCategory.CITY_EXPLORATION, List.of(
                            "nwr[\"tourism\"~\"attraction|information|viewpoint\"]",
                            "nwr[\"place\"=\"square\"]",
                            "way[\"highway\"=\"pedestrian\"]",
                            "nwr[\"historic\"~\"townhall|citywalls\"]",
                            "nwr[\"amenity\"=\"townhall\"]"
                    )
            );

    private POICategoryOsmQueryBuilder() {
    }

    /**
     * Returns the pure tag fragments (without any range or around clause).
     */
    public static List<String> getTagFragments(PoiCategory category) {
        return CATEGORY_TO_FRAGMENTS.getOrDefault(category, List.of());
    }

    /**
     * Builds a full Overpass query for multiple categories combined in one union.
     */
    public static String buildAroundQuery(
            List<PoiCategory> categories,
            double latitude,
            double longitude,
            int radiusMeters
    ) {
        // Collect all tag fragments for all categories
        List<String> fragments = categories.stream()
                .flatMap(cat -> getTagFragments(cat).stream())
                .toList();

        if (fragments.isEmpty()) {
            throw new IllegalArgumentException("No tag fragments found for given categories: " + categories);
        }

        // Attach the (around:radius,lat,lon) filter to each fragment
        String body = fragments.stream()
                .map(f -> f + String.format(Locale.US,
                        "(around:%d,%.6f,%.6f)", radiusMeters, latitude, longitude))
                .collect(Collectors.joining(";\n  ", "(\n  ", ";\n);\n"));

        // Assemble final Overpass query
        return "[out:json][timeout:" + DEFAULT_TIMEOUT_SECONDS + "];\n"
                + body
                + "out center tags;";
    }
}