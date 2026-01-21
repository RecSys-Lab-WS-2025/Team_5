package de.tum.moodtrip_backend.adapter.content.chatbot.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.RouteText;

/**
 * Mapper for parsing AI-generated route text responses into RouteText objects.
 * Supports day-by-day descriptions with multiple response formats.
 */
public class RouteTextMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteTextMapper.class);



    /**
     * Generate mock RouteText for testing (bypasses AI)
     */
    public static RouteText createMockRouteText(String mood, String city, List<EnrichedPoi> pois, int tripDays) {
        LOGGER.debug("Creating mock route text for mood: {}, city: {}, {} days", mood, city, tripDays);

        String title = generateMockTitle(mood, city);
        Map<Integer, String> dayDescriptions = generateMockDayDescriptions(mood, city, pois, tripDays);

        LOGGER.info("Generated mock route - Title: '{}', {} day descriptions",
            title, dayDescriptions.size());

        return new RouteText(title, dayDescriptions);
    }



    // ==================== Private Parsing Methods ====================

    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * DTO for parsing JSON response
     */
    private static class RouteTextDto {
        public String title;
        public Map<String, String> dayDescriptions;
    }

    private static class BatchRouteTextDto {
        // Map keys will be route types (e.g., "BALANCED"), values are the route text objects
        // Using Map<String, RouteTextDto> to catch all dynamic keys
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public Map<String, RouteTextDto> routes = new HashMap<>();
    }

    /**
     * Parse batch AI response string into a Map of RouteType to RouteText.
     */
    public static Map<de.tum.moodtrip_backend.core.model.RouteType, RouteText> parseBatchResponse(String response) {
        LOGGER.info("Parsing batch AI response, length: {} chars", response.length());
        String cleanedResponse = response.trim();
        
        // Remove markdown code blocks if present (e.g. ```json ... ```)
        if (cleanedResponse.startsWith("```")) {
            cleanedResponse = cleanedResponse.replaceAll("^```json", "").replaceAll("^```", "").replaceAll("```$", "").trim();
        }

        Map<de.tum.moodtrip_backend.core.model.RouteType, RouteText> result = new HashMap<>();

        try {
            // Configure loose parsing
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Need to parse as a Map<String, RouteTextDto> because the keys are dynamic enum names
            com.fasterxml.jackson.core.type.TypeReference<HashMap<String, RouteTextDto>> typeRef 
                = new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, RouteTextDto>>() {};
                
            Map<String, RouteTextDto> dtos = objectMapper.readValue(cleanedResponse, typeRef);

            for (Map.Entry<String, RouteTextDto> entry : dtos.entrySet()) {
                String typeKey = entry.getKey();
                RouteTextDto dto = entry.getValue();

                try {
                    de.tum.moodtrip_backend.core.model.RouteType routeType = 
                        de.tum.moodtrip_backend.core.model.RouteType.valueOf(typeKey.toUpperCase());
                    
                    if (dto.title != null && dto.dayDescriptions != null) {
                        // Convert day descriptions keys to Integer
                        Map<Integer, String> intDayDescriptions = new HashMap<>();
                        for (Map.Entry<String, String> dayEntry : dto.dayDescriptions.entrySet()) {
                            try {
                                intDayDescriptions.put(Integer.parseInt(dayEntry.getKey()), dayEntry.getValue());
                            } catch (NumberFormatException e) {
                                LOGGER.warn("Invalid day key in batch JSON for type {}: {}", typeKey, dayEntry.getKey());
                            }
                        }
                        result.put(routeType, new RouteText(dto.title, intDayDescriptions));
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Unknown route type in batch response: {}", typeKey);
                }
            }
            
            LOGGER.info("Successfully parsed batch response for {} types", result.size());
            
        } catch (Exception e) {
            LOGGER.warn("Failed to parse batch JSON response: {}", e.getMessage());
        }
        
        return result;
    }





    // ==================== Mock Generation Methods ====================

    /**
     * Generate varied mock titles based on mood and city
     */
    private static String generateMockTitle(String mood, String city) {
        String formattedMood = capitalizeFirst(mood);
        String formattedCity = city != null && !city.trim().isEmpty() ? city.trim() : "Test Location";

        return switch (mood.toLowerCase()) {
            case "joyful", "happy", "excited", "energized" ->
                String.format("%s %s Adventure", formattedMood, formattedCity);
            case "relaxed", "calm", "peaceful", "tired", "stressed" ->
                String.format("Serene %s Escape", formattedCity);
            case "adventurous", "bold", "exploratory" ->
                String.format("%s Discovery Quest", formattedCity);
            case "contemplative", "thoughtful", "reflective", "sad" ->
                String.format("Mindful %s Journey", formattedCity);
            default ->
                String.format("Mock %s Route in %s", formattedMood, formattedCity);
        };
    }

    /**
     * Generate detailed mock descriptions for each day with specific POI mentions
     */
    private static Map<Integer, String> generateMockDayDescriptions(String mood, String city, List<EnrichedPoi> pois, int tripDays) {
        String formattedCity = city != null && !city.trim().isEmpty() ? city.trim() : "the test location";
        Map<Integer, String> dayDescriptions = new HashMap<>();

        List<EnrichedPoi> effectivePois = (pois != null) ? pois : List.of();
        int totalPois = effectivePois.size();

        for (int day = 1; day <= tripDays; day++) {
            StringBuilder description = new StringBuilder();

            // Intro based on mood and day
            String intro = switch (mood.toLowerCase()) {
                case "joyful", "happy" ->
                    day == 1 ? String.format("Begin your joyful journey through %s, ", formattedCity)
                             : "Continue your energetic exploration, ";
                case "relaxed", "calm" ->
                    day == 1 ? String.format("Start your serene exploration of %s, ", formattedCity)
                             : "Continue your peaceful journey, ";
                case "adventurous", "bold" ->
                    day == 1 ? String.format("Launch your exciting adventure across %s, ", formattedCity)
                             : "Continue your bold exploration, ";
                case "contemplative", "thoughtful" ->
                    day == 1 ? String.format("Embark on a mindful journey through %s, ", formattedCity)
                             : "Continue your reflective exploration, ";
                default ->
                    day == 1 ? String.format("Discover %s through this carefully curated route, ", formattedCity)
                             : "Continue exploring the planned destinations, ";
            };

            description.append(intro);

            // Find POIs for this day using the same formula as GeoJsonRouteMapper
            int poisInDay = 0;
            for (int i = 0; i < totalPois; i++) {
                int poiDay = (int) Math.floor((double) i * tripDays / totalPois) + 1;
                if (poiDay == day) {
                    EnrichedPoi poi = effectivePois.get(i);
                    String poiName = poi.poi().name();

                    String connector = switch (poisInDay) {
                        case 0 -> "visiting ";
                        case 1 -> "then exploring ";
                        default -> "and continuing to ";
                    };

                    description.append(connector).append(poiName);
                    poisInDay++;

                    if (i < totalPois - 1) {
                        // Check if next POI is also in this day
                        int nextPoiDay = (int) Math.floor((double) (i + 1) * tripDays / totalPois) + 1;
                        if (nextPoiDay == day) {
                            description.append(", ");
                        }
                    }
                }
            }

            if (poisInDay == 0) {
                description.append("enjoying the local atmosphere and taking time to relax");
            }
            description.append(".");

            dayDescriptions.put(day, description.toString());
        }

        return dayDescriptions;
    }

    // ==================== Helper Methods ====================

    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }


}
