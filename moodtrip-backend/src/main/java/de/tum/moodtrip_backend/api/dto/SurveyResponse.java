package de.tum.moodtrip_backend.api.dto;

import org.geojson.FeatureCollection;

import de.tum.moodtrip_backend.core.model.RouteStatus;

public record SurveyResponse(
        RouteStatus routeStatus,
        FeatureCollection route,
        String spotifyPlaylistLink,
        String userMessage
) {
    public static SurveyResponse success(FeatureCollection route, String spotifyPlaylistLink) {
        return new SurveyResponse(RouteStatus.SUCCEEDED, route, spotifyPlaylistLink, null);
    }

    public static SurveyResponse failure(String userMessage) {
        return new SurveyResponse(RouteStatus.FAILED, null, null, userMessage);
    }
}
