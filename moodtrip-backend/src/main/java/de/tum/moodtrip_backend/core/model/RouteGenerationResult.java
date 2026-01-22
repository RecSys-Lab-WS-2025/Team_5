package de.tum.moodtrip_backend.core.model;

public record RouteGenerationResult(
        RouteStatus status,
        PoiRouteResult route,
        RouteType routeType,
        String userMessage
) {
    public static RouteGenerationResult success(PoiRouteResult route) {
        return new RouteGenerationResult(RouteStatus.SUCCEEDED, route, null, null);
    }

    public static RouteGenerationResult success(PoiRouteResult route, RouteType routeType) {
        return new RouteGenerationResult(RouteStatus.SUCCEEDED, route, routeType, null);
    }

    public static RouteGenerationResult failure(String userMessage) {
        return new RouteGenerationResult(RouteStatus.FAILED, null, null, userMessage);
    }
}
