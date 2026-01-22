package de.tum.moodtrip_backend.core.model;

import java.util.List;

/**
 * Context for generating a single route's description within a batch request.
 */
public record RouteGenerationContext(
    RouteType routeType,
    List<EnrichedPoi> pois,
    int tripDays
) {}
