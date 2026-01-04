package de.tum.moodtrip_backend.adapter.maps.osm.builder;

import java.util.Locale;

/**
 * Builds the unified Overpass query used for POI candidate generation.
 * <p>
 * The query is intentionally broad and category-agnostic; POIs are categorized
 * downstream using tag-based logic. Only the latitude, longitude and radius
 * placeholders are interpolated here.
 */
public final class OverpassQueryBuilder {

    private static final int DEFAULT_TIMEOUT_SECONDS = 25;

    /**
     * Shared query that fetches a broad set of POIs around a location.
     */
    private static final String QUERY_TEMPLATE = """
            [out:json][timeout:%1$d];

            (
              /* NATURE / RELAXATION */
              nwr["natural"](around:%2$d,%3$.6f,%4$.6f);
              nwr["leisure"~"park|garden|nature_reserve|common|recreation_ground"](around:%2$d,%3$.6f,%4$.6f);
              nwr["tourism"="viewpoint"](around:%2$d,%3$.6f,%4$.6f);

              /* HISTORY & CULTURE */
              nwr["historic"~"monument|memorial|castle|ruins|archaeological_site|wayside_cross|wayside_shrine"](around:%2$d,%3$.6f,%4$.6f);
              nwr["tourism"~"museum|gallery|attraction|artwork"](around:%2$d,%3$.6f,%4$.6f);
              nwr["amenity"~"theatre|arts_centre|cinema|library"](around:%2$d,%3$.6f,%4$.6f);
              nwr["amenity"="place_of_worship"](around:%2$d,%3$.6f,%4$.6f);

              /* ADVENTURE */
              nwr["leisure"~"sports_centre|climbing|water_park|golf_course|pitch|swimming_pool|fitness_centre|stadium|track"](around:%2$d,%3$.6f,%4$.6f);
              nwr["sport"](around:%2$d,%3$.6f,%4$.6f);
              nwr["tourism"="zoo"](around:%2$d,%3$.6f,%4$.6f);

              /* FOOD & CULINARY */
              nwr["amenity"~"restaurant|cafe|bar|fast_food|biergarten"](around:%2$d,%3$.6f,%4$.6f);

              /* SHOPPING (curated) */
              nwr["shop"~"supermarket|department_store|mall|clothes|shoes|jewelry|bakery|butcher|convenience|chemist|books|gift|florist"](around:%2$d,%3$.6f,%4$.6f);
              nwr["amenity"="marketplace"](around:%2$d,%3$.6f,%4$.6f);
            )->.candidates;

            /* Keep if it has wikipedia/website/wikimedia_commons/image */
            (
              nwr.candidates(if:
                t["wikipedia"] || t["wikidata"] || t["wikimedia_commons"] || t["website"] || t["image"]
              );
            )->.filtered;

            out center tags qt 2000;
            """;

    private OverpassQueryBuilder() {
    }

    public static String buildAroundQuery(double latitude, double longitude, int radiusMeters) {
        return String.format(Locale.US, QUERY_TEMPLATE,
                DEFAULT_TIMEOUT_SECONDS,
                radiusMeters,
                latitude,
                longitude);
    }
}
