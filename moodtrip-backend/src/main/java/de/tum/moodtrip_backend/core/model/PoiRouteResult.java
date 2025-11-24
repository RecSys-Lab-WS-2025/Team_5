package de.tum.moodtrip_backend.core.model;

import java.util.List;

public record PoiRouteResult(
        List<EnrichedPoi> pois,
        Route route
) {
}
