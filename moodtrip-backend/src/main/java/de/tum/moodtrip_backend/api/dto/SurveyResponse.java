package de.tum.moodtrip_backend.api.dto;

import org.geojson.FeatureCollection;

public record SurveyResponse(
    FeatureCollection route,
    String spotifyPlaylistLink
) {}
