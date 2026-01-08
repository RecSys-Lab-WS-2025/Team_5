package de.tum.moodtrip_backend.api.mapper;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.PoiRouteResult;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiScore;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.model.RouteCoordinate;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public final class GeoJsonRouteMapper {
    /**
     * Build a GeoJSON FeatureCollection from enriched POIs and an optional route.
     *
     * @param poiRouteResult domain object containing enriched POIs and the route
     * @param emotion current emotion for styling
     * @param tripDays total duration of the trip in days
     * @return GeoJSON FeatureCollection
     */
    public FeatureCollection toFeatureCollection(PoiRouteResult poiRouteResult, String emotion, int tripDays) {
        FeatureCollection featureCollection = new FeatureCollection();

        if (poiRouteResult == null) {
            return featureCollection;
        }

        int totalPois = poiRouteResult.pois() != null ? poiRouteResult.pois().size() : 0;

        // POI features
        if (poiRouteResult.pois() != null) {
            for (int i = 0; i < totalPois; i++) {
                EnrichedPoi enrichedPoi = poiRouteResult.pois().get(i);
                Poi poi = enrichedPoi.poi();
                Feature poiFeature = new Feature();

                // GeoJSON uses (lon, lat) order
                Point point = new Point(poi.longitude(), poi.latitude());
                poiFeature.setGeometry(point);

                // Basic properties
                poiFeature.setProperty("type", "poi");
                poiFeature.setProperty("osmId", poi.osmId());
                poiFeature.setProperty("osmType", poi.osmType().name());
                poiFeature.setProperty("name", poi.name());
                poiFeature.setProperty("category", poi.category().name());

                // Assign a day based on index
                // Distribution: Round-robin or chunked. Chunked is better for itinerary.
                int day = (int) Math.floor((double) i * tripDays / totalPois) + 1;
                poiFeature.setProperty("day", day);

                // Display name, description, image
                poiFeature.setProperty("displayName", enrichedPoi.displayName());
                poiFeature.setProperty("description", enrichedPoi.description());
                poiFeature.setProperty("imageUrl", enrichedPoi.imageUrl());

                PoiScore score = enrichedPoi.score();
                if (score != null) {
                    poiFeature.setProperty("finalScore", score.finalScore());
                    poiFeature.setProperty("categoryScore", score.categoryScore());
                    poiFeature.setProperty("tagScore", score.tagScore());
                    poiFeature.setProperty("distanceScore", score.distanceScore());
                    poiFeature.setProperty("distanceMeters", score.distanceMeters());
                    poiFeature.setProperty("emotionContributions", score.emotionContributions());
                }

                // All original OSM tags as nested object
                Map<String, String> tags = poi.tags();
                if (tags != null && !tags.isEmpty()) {
                    poiFeature.setProperty("tags", tags);
                }

                featureCollection.add(poiFeature);
            }
        }

        // Route feature (optional)
        Route route = poiRouteResult.route();
        if (route != null && route.geometry() != null && !route.geometry().isEmpty()) {
            Feature routeFeature = new Feature();

            LineString lineString = new LineString();
            for (RouteCoordinate coord : route.geometry()) {
                // GeoJSON order = (lon, lat)
                lineString.add(new LngLatAlt(coord.lon(), coord.lat()));
            }

            routeFeature.setGeometry(lineString);
            routeFeature.setProperty("type", "route");
            routeFeature.setProperty("distanceMeters", route.distanceMeters());
            routeFeature.setProperty("durationSeconds", route.durationSeconds());
            routeFeature.setProperty("emotion", emotion);
            routeFeature.setProperty("tripDays", tripDays);


            List<Map<String, Object>> dailyStats = new ArrayList<>();
            double walkingSpeedMps = 5000.0 / 3600.0;
            
            for (int d = 1; d <= tripDays; d++) {
                double dayDistance = 0;
                int dayPoisCount = 0;

                for (int i = 0; i < totalPois; i++) {
                    int poiDay = (int) Math.floor((double) i * tripDays / totalPois) + 1;
                    if (poiDay == d) {
                        dayPoisCount++;
                        if (i > 0 && i <= route.legDistances().size()) {
                            dayDistance += route.legDistances().get(i - 1);
                        }
                    }
                }
                
                Map<String, Object> dayStat = new HashMap<>();
                dayStat.put("day", d);
                dayStat.put("distanceMeters", Math.round(dayDistance));
                dayStat.put("durationSeconds", Math.round((dayDistance / walkingSpeedMps) + (dayPoisCount * 45 * 60)));
                dailyStats.add(dayStat);
            }
            routeFeature.setProperty("dailyStats", dailyStats);

            featureCollection.add(routeFeature);
        }

        return featureCollection;
    }
}
