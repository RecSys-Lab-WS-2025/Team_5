package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.RouteText;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RouteDescriptionGeneratorPort {

    /**
     * Generate a personalized title and day-by-day descriptions for a route based on the user's mood and POIs.
     *
     * @param mood the user's mood for the trip
     * @param city the destination city
     * @param pois the list of points of interest in the route
     * @param tripDays the number of days for the trip
     * @param isMocked whether to use mock data instead of LLM
     * @return a Mono emitting a RouteText object containing the title and day descriptions
     */
    Mono<RouteText> generateRouteText(String mood, String city, List<EnrichedPoi> pois, int tripDays, boolean isMocked);
}