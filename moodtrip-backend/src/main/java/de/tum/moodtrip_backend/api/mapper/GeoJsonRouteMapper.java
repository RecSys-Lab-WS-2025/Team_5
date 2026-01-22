package de.tum.moodtrip_backend.api.mapper;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.PoiRouteResult;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiScore;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.model.RouteCoordinate;
import de.tum.moodtrip_backend.core.model.RouteType;
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

import static de.tum.moodtrip_backend.core.service.RouteService.POI_VISITING_TIME_SECONDS;
import static de.tum.moodtrip_backend.core.service.RouteService.WALKING_SPEED_MPS;

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
        return toFeatureCollection(poiRouteResult, emotion, tripDays, null);
    }

    /**
     * Build a GeoJSON FeatureCollection from enriched POIs and an optional route with route type.
     *
     * @param poiRouteResult domain object containing enriched POIs and the route
     * @param emotion current emotion for styling
     * @param tripDays total duration of the trip in days
     * @param routeType the type of route (emotion-focused, category-focused, balanced)
     * @return GeoJSON FeatureCollection
     */
    public FeatureCollection toFeatureCollection(PoiRouteResult poiRouteResult, String emotion, int tripDays, RouteType routeType) {
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

                //chunked to evenly spread POIs
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

            // Add route type properties
            if (routeType != null) {
                routeFeature.setProperty("routeType", routeType.name());
                routeFeature.setProperty("routeTypeTitle", routeType.getDisplayTitle());
                routeFeature.setProperty("routeTypeDescription", routeType.getDescription());
            }

            // Add title and day descriptions to the route feature
            if (route.title() != null) {
                routeFeature.setProperty("name", route.title());
            }
            if (route.dayDescriptions() != null && !route.dayDescriptions().isEmpty()) {
                // Convert Map<Integer, String> to Map<String, String> for JSON serialization
                Map<String, String> dayDescriptionsStr = new HashMap<>();
                for (Map.Entry<Integer, String> entry : route.dayDescriptions().entrySet()) {
                    dayDescriptionsStr.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                routeFeature.setProperty("dayDescriptions", dayDescriptionsStr);
            }

            List<Map<String, Object>> dailyStats = new ArrayList<>();
            double totalActiveDistance = 0;
            double totalActiveDuration = 0;

            for (int d = 1; d <= tripDays; d++) {
                double dayDistance = 0;
                double dayDuration = 0;
                int dayPoisCount = 0;

                for (int i = 0; i < totalPois; i++) {
                    int poiDay = (int) Math.floor((double) i * tripDays / totalPois) + 1;
                    if (poiDay == d) {
                        dayPoisCount++;
                        // Only count leg[i] if BOTH POI i and POI i+1 are in the same day
                        // leg[i] represents the distance from POI i to POI i+1
                        if (i < route.legDistances().size()) {
                            int nextPoiDay = (int) Math.floor((double) (i + 1) * tripDays / totalPois) + 1;
                            if (nextPoiDay == d) {
                                // Both POIs are in the same day, count this leg
                                dayDistance += route.legDistances().get(i);
                                dayDuration += route.legDurations().get(i);
                            }
                            // Cross-day legs are not counted (user might use transport between days)
                        }
                    }
                }

                Map<String, Object> dayStat = new HashMap<>();
                dayStat.put("day", d);
                dayStat.put("distanceMeters", Math.round(dayDistance));
                double totalDayTime = dayDuration + (dayPoisCount * POI_VISITING_TIME_SECONDS);
                dayStat.put("durationSeconds", Math.round(totalDayTime));
                dailyStats.add(dayStat);

                totalActiveDistance += dayDistance;
                totalActiveDuration += totalDayTime;
            }
            routeFeature.setProperty("dailyStats", dailyStats);
            
            // Overwrite total distance/duration with the sum of ACTIVE daily parts
            // preventing the inclusion of long cross-day travel legs in the difficulty calculation
            routeFeature.setProperty("distanceMeters", Math.round(totalActiveDistance));
            routeFeature.setProperty("durationSeconds", Math.round(totalActiveDuration));
            
            featureCollection.add(routeFeature);
        }

        return featureCollection;
    }
}