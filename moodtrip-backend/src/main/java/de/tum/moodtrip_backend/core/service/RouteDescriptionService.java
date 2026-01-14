package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.RouteText;
import de.tum.moodtrip_backend.core.port.RouteDescriptionGeneratorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RouteDescriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteDescriptionService.class);
    private final RouteDescriptionGeneratorPort routeDescriptionGeneratorPort;

    public RouteDescriptionService(RouteDescriptionGeneratorPort routeDescriptionGeneratorPort) {
        this.routeDescriptionGeneratorPort = routeDescriptionGeneratorPort;
    }

    public Mono<RouteText> generateRouteText(String mood, String city, List<EnrichedPoi> pois, boolean isMocked) {
        LOGGER.info("Generating route text for mood: {}, city: {}, with {} POIs (isMocked={})", mood, city, pois.size(), isMocked);

        // Call the port to generate the route text
        return routeDescriptionGeneratorPort.generateRouteText(mood, city, pois, isMocked)
                .onErrorResume(error -> {
                    LOGGER.warn("Failed to generate route text with AI, falling back to default: {}", error.getMessage());
                    return Mono.fromCallable(() -> fallbackRouteText(mood, city));
                });
    }

    /**
     * Fallback method to generate a simple route text when AI generation fails
     */
    private RouteText fallbackRouteText(String mood, String city) {
        String title = String.format("%s Trip in %s", capitalizeFirst(mood), city);
        String description = String.format("A personalized route for your %s mood exploring %s.", mood.toLowerCase(), city);
        return new RouteText(title, description);
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}