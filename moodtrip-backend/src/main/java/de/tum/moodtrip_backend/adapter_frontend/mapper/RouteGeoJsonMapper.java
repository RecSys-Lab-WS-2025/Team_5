package de.tum.moodtrip_backend.adapter_frontend.mapper;

import de.tum.moodtrip_backend.core.model.Route;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.MultiLineString;
import org.springframework.stereotype.Component;

@Component
public final class RouteGeoJsonMapper {

    /**
     * Build a proper GeoJSON FeatureCollection using the org.geojson library
     * (geojson-jackson), letting Jackson handle serialization.
     * Geometry coordinates are [lon, lat] as per GeoJSON spec.
     */
    public FeatureCollection toFeatureCollection(Route route) {
        // Build MultiLineString from route lines
        MultiLineString multi = new MultiLineString();
        for (var line : route.lines()) {
            LineString ls = new LineString();
            for (var c : line) {
                ls.add(new LngLatAlt(c.lon(), c.lat()));
            }
            multi.add(ls.getCoordinates());
        }

        Feature feature = new Feature();
        feature.setGeometry(multi);
        feature.setProperty("relationId", route.relationId());
        feature.setProperty("name", route.name());

        FeatureCollection fc = new FeatureCollection();
        fc.add(feature);
        return fc;
    }
}
