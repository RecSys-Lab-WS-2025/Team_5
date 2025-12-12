package de.tum.moodtrip_backend.core.model;

public record RouteGenerationResult(
        RouteStatus status,
        PoiRouteResult route,
        String userMessage
) {
    public static RouteGenerationResult success(PoiRouteResult route) {
        return new RouteGenerationResult(RouteStatus.SUCCEEDED, route, null);
    }

    public static RouteGenerationResult failure(String userMessage) {
        return new RouteGenerationResult(RouteStatus.FAILED, null, userMessage);
    }
}
