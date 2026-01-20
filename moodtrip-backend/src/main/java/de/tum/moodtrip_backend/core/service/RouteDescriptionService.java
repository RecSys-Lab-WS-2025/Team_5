package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.RouteText;
import de.tum.moodtrip_backend.core.port.RouteDescriptionGeneratorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteDescriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteDescriptionService.class);
    private final RouteDescriptionGeneratorPort routeDescriptionGeneratorPort;

    public RouteDescriptionService(RouteDescriptionGeneratorPort routeDescriptionGeneratorPort) {
        this.routeDescriptionGeneratorPort = routeDescriptionGeneratorPort;
    }

    public Mono<RouteText> generateRouteText(String mood, String city, List<EnrichedPoi> pois, int tripDays, boolean isMocked) {
        LOGGER.info("Generating route text for mood: {}, city: {}, with {} POIs, {} days (isMocked={})",
            mood, city, pois.size(), tripDays, isMocked);

        // Call the port to generate the route text
        return routeDescriptionGeneratorPort.generateRouteText(mood, city, pois, tripDays, isMocked)
                .onErrorResume(error -> {
                    LOGGER.warn("Failed to generate route text with AI, falling back to default: {}", error.getMessage());
                    return Mono.fromCallable(() -> fallbackRouteText(mood, city, tripDays));
                });
    }

    /**
     * Fallback method to generate simple route text when AI generation fails
     */
    private RouteText fallbackRouteText(String mood, String city, int tripDays) {
        String title = String.format("%s Trip in %s", capitalizeFirst(mood), city);
        Map<Integer, String> dayDescriptions = new HashMap<>();
        for (int day = 1; day <= tripDays; day++) {
            dayDescriptions.put(day, String.format("Day %d: A personalized route for your %s mood exploring %s.",
                day, mood.toLowerCase(), city));
        }
        return new RouteText(title, dayDescriptions);
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}