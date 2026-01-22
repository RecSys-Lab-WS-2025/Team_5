package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.RouteGenerationContext;
import de.tum.moodtrip_backend.core.model.RouteText;
import de.tum.moodtrip_backend.core.model.RouteType;
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





    public Mono<Map<RouteType, RouteText>> generateBatchRouteText(String mood, String city, List<RouteGenerationContext> contexts, boolean isMocked) {
        LOGGER.info("Generating batch route text for mood: {}, city: {}, with {} contexts (isMocked={})",
            mood, city, contexts != null ? contexts.size() : 0, isMocked);
        
        return routeDescriptionGeneratorPort.generateBatchRouteText(mood, city, contexts, isMocked)
                .map(results -> {
                    // Enrich titles with prefixes if needed, similar to single route generation
                    Map<RouteType, RouteText> enrichedResults = new HashMap<>();
                    if (contexts != null) {
                        for (RouteGenerationContext ctx : contexts) {
                             RouteText text = results.get(ctx.routeType());
                             if (text != null) {
                                 String typePrefix = ctx.routeType().getDisplayTitle();
                                 // Check if title already starts with prefix to avoid double prefixing if AI did it
                                 String finalTitle = text.title();
                                 if (!finalTitle.startsWith(typePrefix)) {
                                     finalTitle = typePrefix + ": " + finalTitle;
                                 }
                                 enrichedResults.put(ctx.routeType(), new RouteText(finalTitle, text.dayDescriptions()));
                             } else {
                                 // Fallback for missing type in response
                                 LOGGER.warn("Missing batch result for type {}, using fallback", ctx.routeType());
                                 enrichedResults.put(ctx.routeType(), fallbackRouteTextForType(mood, city, ctx.tripDays(), ctx.routeType()));
                             }
                        }
                    }
                    return enrichedResults;
                })
                .onErrorResume(error -> {
                    LOGGER.warn("Failed to generate batch route text with AI, falling back to defaults: {}", error.getMessage());
                    Map<RouteType, RouteText> fallbacks = new HashMap<>();
                    if (contexts != null) {
                        for (RouteGenerationContext ctx : contexts) {
                            fallbacks.put(ctx.routeType(), fallbackRouteTextForType(mood, city, ctx.tripDays(), ctx.routeType()));
                        }
                    }
                    return Mono.just(fallbacks);
                });
    }

    private RouteText fallbackRouteTextForType(String mood, String city, int tripDays, RouteType routeType) {
        String baseTitle = String.format("%s Trip in %s", capitalizeFirst(mood), city);
        String title = routeType != null ? routeType.getDisplayTitle() + ": " + baseTitle : baseTitle;
        Map<Integer, String> dayDescriptions = new HashMap<>();
        for (int day = 1; day <= tripDays; day++) {
            String typeDesc = routeType != null ? routeType.getDescription() + " - " : "";
            dayDescriptions.put(day, String.format("Day %d: %sA personalized route for your %s mood exploring %s.",
                day, typeDesc, mood.toLowerCase(), city));
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