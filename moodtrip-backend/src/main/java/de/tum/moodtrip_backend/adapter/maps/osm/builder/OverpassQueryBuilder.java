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
               /* NATURE */
               nwr["boundary"="protected_area"](around:%2$d,%3$.6f,%4$.6f);
               nwr["protect_class"](around:%2$d,%3$.6f,%4$.6f);
               nwr["leisure"="nature_reserve"](around:%2$d,%3$.6f,%4$.6f);
               nwr["natural"~"wood|grassland|wetland|water|heath|sand|scrub"](around:%2$d,%3$.6f,%4$.6f);
               nwr["landuse"~"forest|meadow"](around:%2$d,%3$.6f,%4$.6f);
               nwr["tourism"="viewpoint"](around:%2$d,%3$.6f,%4$.6f);

               /* RELAXATION */
               nwr["leisure"~"park|garden|recreation_ground|picnic_site"](around:%2$d,%3$.6f,%4$.6f);
               nwr["tourism"="picnic_site"](around:%2$d,%3$.6f,%4$.6f);
               nwr["leisure"="playground"](around:%2$d,%3$.6f,%4$.6f);

               /* HISTORY & CULTURE */
               nwr["historic"~"monument|memorial|castle|ruins|archaeological_site|fort|heritage_site"](around:%2$d,%3$.6f,%4$.6f);
               nwr["tourism"~"museum|gallery|attraction|artwork|heritage_site"](around:%2$d,%3$.6f,%4$.6f);
               nwr["amenity"~"theatre|arts_centre"](around:%2$d,%3$.6f,%4$.6f);
               nwr["amenity"="place_of_worship"](around:%2$d,%3$.6f,%4$.6f);
               nwr["building"="historic"](around:%2$d,%3$.6f,%4$.6f);

               /* ADVENTURE / SPORT & OUTDOOR ACTIVITIES */
               nwr["leisure"~"sports_centre|swimming_pool|ice_rink|horse_riding"](around:%2$d,%3$.6f,%4$.6f);
               nwr["leisure"="high_ropes_course"](around:%2$d,%3$.6f,%4$.6f);
               nwr["leisure"="track"]["sport"](around:%2$d,%3$.6f,%4$.6f);

               nwr["tourism"="alpine_hut"](around:%2$d,%3$.6f,%4$.6f);
               nwr["tourism"="camp_site"](around:%2$d,%3$.6f,%4$.6f);
               nwr["tourism"="theme_park"](around:%2$d,%3$.6f,%4$.6f);

               /* FOOD & CULINARY */
               nwr["amenity"~"restaurant|cafe|biergarten|ice_cream|tea_room"](around:%2$d,%3$.6f,%4$.6f);
               nwr["amenity"="pub"](around:%2$d,%3$.6f,%4$.6f);
               nwr["shop"~"confectionery|cheese|delicatessen"](around:%2$d,%3$.6f,%4$.6f);

               /* SHOPPING */
               nwr["shop"~"department_store|mall|antique|art|craft|handicraft|bookstore|music"](around:%2$d,%3$.6f,%4$.6f);
               nwr["amenity"="marketplace"](around:%2$d,%3$.6f,%4$.6f);
             )
             ->.candidates;

             /* Keep if it has relevant metadata */
             (
               nwr.candidates(if:
                 t["wikipedia"]
                 || t["wikidata"]
                 || t["wikimedia_commons"]
                 || t["website"]
                 || t["image"]
                 || t["name"]
               );
             )
             ->.filtered;

             out center tags qt 10000;
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
