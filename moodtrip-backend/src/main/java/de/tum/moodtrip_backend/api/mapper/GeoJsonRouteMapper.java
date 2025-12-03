package de.tum.moodtrip_backend.api.mapper;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.PoiRouteResult;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.model.RouteCoordinate;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;

import java.util.Map;

public final class GeoJsonRouteMapper {
    /**
     * Build a GeoJSON FeatureCollection from enriched POIs and an optional route.
     *
     * @param poiRouteResult domain object containing enriched POIs and the route
     * @return GeoJSON FeatureCollection
     */
    public static FeatureCollection toFeatureCollection(PoiRouteResult poiRouteResult) {
        FeatureCollection featureCollection = new FeatureCollection();

        if (poiRouteResult == null) {
            return featureCollection;
        }

        // POI features
        if (poiRouteResult.pois() != null) {
            for (EnrichedPoi enrichedPoi : poiRouteResult.pois()) {
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

                // Display name, description, image
                poiFeature.setProperty("displayName", enrichedPoi.displayName());
                poiFeature.setProperty("description", enrichedPoi.description());
                poiFeature.setProperty("imageUrl", enrichedPoi.imageUrl());

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

            featureCollection.add(routeFeature);
        }

        return featureCollection;
    }
}