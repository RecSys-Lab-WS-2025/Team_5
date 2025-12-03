package de.tum.moodtrip_backend.adapter.maps.osm.builder;

import de.tum.moodtrip_backend.core.model.POICategory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class POICategoryOsmQueryBuilder {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private static final Map<POICategory, List<String>> CATEGORY_TO_FRAGMENTS =
            Map.of(
                    POICategory.NATURE, List.of(
                            "nwr[\"leisure\"~\"park|garden|nature_reserve\"]",
                            "nwr[\"boundary\"=\"protected_area\"]",
                            "nwr[\"natural\"~\"wood|forest|wetland|heath|scrub|grassland|meadow|water|lake|fell|moor|peak|valley\"]",
                            "nwr[\"tourism\"~\"picnic_site\"]"
                    ),

                    POICategory.HISTORY_AND_CULTURE, List.of(
                            "nwr[\"historic\"]",
                            "nwr[\"heritage\"]",
                            "nwr[\"memorial\"]",
                            "nwr[\"tourism\"=\"attraction\"][\"historic\"]",
                            "nwr[\"amenity\"=\"place_of_worship\"]"
                    ),

                    POICategory.ADVENTURE, List.of(
                            "nwr[\"leisure\"~\"sports_centre|pitch|stadium|water_park|adventure_park\"]",
                            "nwr[\"tourism\"=\"theme_park\"]",
                            "nwr[\"sport\"~\"climbing|skiing|snowboard|surfing|diving|canoe|kayak|rafting|skateboard|mtb|paragliding\"]"
                    ),

                    POICategory.RELAXATION, List.of(
                            "nwr[\"leisure\"~\"park|garden|spa\"]",
                            "nwr[\"amenity\"~\"spa|sauna\"]",
                            "nwr[\"tourism\"~\"hotel|guest_house|resort\"]"
                    ),

                    POICategory.FOOD_AND_CULINARY, List.of(
                            "nwr[\"amenity\"~\"restaurant|cafe|fast_food|biergarten|pub|bar|food_court|ice_cream|brewery\"]",
                            "nwr[\"craft\"~\"winery|brewery\"]",
                            "nwr[\"shop\"~\"bakery|confectionery|deli|butcher\"]"
                    ),

                    POICategory.NIGHTLIFE, List.of(
                            "nwr[\"amenity\"~\"bar|pub|biergarten|nightclub|casino|music_venue\"]"
                    ),

                    POICategory.ART_AND_MUSEUMS, List.of(
                            "nwr[\"tourism\"~\"museum|gallery|artwork\"]",
                            "nwr[\"amenity\"~\"arts_centre|theatre|cinema\"]",
                            "nwr[\"historic\"=\"artwork\"]"
                    ),

                    POICategory.SHOPPING, List.of(
                            "nwr[\"shop\"~\"mall|department_store|supermarket|clothes|shoes|electronics|books|gift|boutique\"]",
                            "nwr[\"amenity\"=\"marketplace\"]",
                            "nwr[\"landuse\"=\"retail\"]"
                    ),

                    POICategory.BEACH_AND_COAST, List.of(
                            "nwr[\"natural\"~\"beach|coastline|bay|dune\"]",
                            "nwr[\"tourism\"=\"beach_resort\"]",
                            "nwr[\"leisure\"=\"beach_resort\"]",
                            "nwr[\"place\"~\"island|islet\"]"
                    ),

                    POICategory.CITY_EXPLORATION, List.of(
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
    public static List<String> getTagFragments(POICategory category) {
        return CATEGORY_TO_FRAGMENTS.getOrDefault(category, List.of());
    }

    /**
     * Builds a full Overpass query for a single category using an (around:radius,lat,lon) filter.
     * <p>
     * Example output:
     * <p>
     * [out:json][timeout:60];
     * (
     * nwr["natural"~"beach|coastline|bay|dune"](around:1000,48.137400,11.575500);
     * ...
     * );
     * out center tags;
     */
    public static String buildAroundQuery(
            POICategory category,
            double latitude,
            double longitude,
            int radiusMeters
    ) {
        return buildAroundQuery(List.of(category), latitude, longitude, radiusMeters);
    }

    /**
     * Builds a full Overpass query for multiple categories combined in one union.
     */
    public static String buildAroundQuery(
            List<POICategory> categories,
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
                + "out center tags 5;";
    }
}