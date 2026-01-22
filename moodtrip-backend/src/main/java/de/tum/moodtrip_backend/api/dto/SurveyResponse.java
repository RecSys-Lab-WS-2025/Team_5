package de.tum.moodtrip_backend.api.dto;

import org.geojson.FeatureCollection;

import de.tum.moodtrip_backend.core.model.RouteStatus;

import java.util.List;

public record SurveyResponse(
        RouteStatus routeStatus,
        FeatureCollection route,           // Keep for backward compatibility
        List<FeatureCollection> routes,    // NEW: all 3 routes
        String spotifyPlaylistLink,
        String userMessage
) {
    public static SurveyResponse success(FeatureCollection route, String spotifyPlaylistLink) {
        return new SurveyResponse(RouteStatus.SUCCEEDED, route, null, spotifyPlaylistLink, null);
    }

    public static SurveyResponse successMultiple(List<FeatureCollection> routes, String spotifyPlaylistLink) {
        // For backward compatibility, also set the first route as the primary route
        FeatureCollection primaryRoute = routes != null && !routes.isEmpty() ? routes.get(0) : null;
        return new SurveyResponse(RouteStatus.SUCCEEDED, primaryRoute, routes, spotifyPlaylistLink, null);
    }

    public static SurveyResponse failure(String userMessage) {
        return new SurveyResponse(RouteStatus.FAILED, null, null, null, userMessage);
    }
}
