package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.RouteText;
import de.tum.moodtrip_backend.core.model.RouteType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RouteDescriptionGeneratorPort {


    /**
     * Generates route text for multiple routes in a single batch request.
     */
    Mono<java.util.Map<RouteType, RouteText>> generateBatchRouteText(String mood, String city, java.util.List<de.tum.moodtrip_backend.core.model.RouteGenerationContext> contexts, boolean isMocked);
}