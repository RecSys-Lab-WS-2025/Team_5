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
     * Parse AI response string into RouteText with day descriptions
     * Supports JSON format with dayDescriptions field
     */
    public static RouteText fromAiResponse(String response, int tripDays) {
        LOGGER.info("Parsing AI response into RouteText with {} days, response length: {} chars", tripDays, response.length());

        String cleanedResponse = response.trim();

        // Try JSON format first
        RouteText jsonResult = tryParseJsonFormat(cleanedResponse, tripDays);
        if (jsonResult != null) {
            return jsonResult;
        }

        // Fallback: create default day descriptions
        return parseFallback(cleanedResponse, tripDays);
    }

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

    /**
     * Create error RouteText for invalid inputs
     */
    public static RouteText createErrorRouteText(String errorMessage, String city, int tripDays) {
        String title = "Error Route";
        Map<Integer, String> dayDescriptions = new HashMap<>();
        String errorDesc = String.format(
            "Unable to generate route: %s. This is a mock response for error handling demonstration in %s.",
            errorMessage,
            city != null ? city : "unknown location"
        );

        for (int day = 1; day <= tripDays; day++) {
            dayDescriptions.put(day, errorDesc);
        }

        LOGGER.warn("Creating error route text: {}", errorMessage);
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

    /**
     * Attempt to parse JSON format with dayDescriptions:
     * {"title": "...", "dayDescriptions": {"1": "...", "2": "..."}}
     */
    private static RouteText tryParseJsonFormat(String response, int tripDays) {
        if (!response.trim().startsWith("{")) {
             LOGGER.debug("Not JSON format (does not start with brace)");
             return null;
        }

        try {
            // Configure loose parsing to handle potential AI inconsistencies
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            objectMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            RouteTextDto dto = objectMapper.readValue(response, RouteTextDto.class);
            
            if (dto.title != null && dto.dayDescriptions != null && !dto.dayDescriptions.isEmpty()) {
                // Convert Map<String, String> to Map<Integer, String>
                Map<Integer, String> intDayDescriptions = new HashMap<>();
                for (Map.Entry<String, String> entry : dto.dayDescriptions.entrySet()) {
                    try {
                        intDayDescriptions.put(Integer.parseInt(entry.getKey()), entry.getValue());
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid day key in JSON: {}", entry.getKey());
                    }
                }
                
                LOGGER.info("Successfully parsed JSON format - Title: '{}', {} day descriptions",
                    dto.title, intDayDescriptions.size());
                return new RouteText(dto.title, intDayDescriptions);
            }
             
            LOGGER.warn("JSON parsed but missing required fields");

        } catch (Exception e) {
            LOGGER.warn("Failed to parse as JSON format: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Fallback parsing: create generic day descriptions from response
     */
    private static RouteText parseFallback(String response, int tripDays) {
        String fallbackTitle = "Personalized " + extractMoodFromResponse(response) + " Journey";

        Map<Integer, String> dayDescriptions = new HashMap<>();
        // Split response into days if possible, otherwise use the whole response for day 1
        for (int day = 1; day <= tripDays; day++) {
            dayDescriptions.put(day, String.format("Day %d of your journey. %s", day,
                tripDays == 1 ? response : "Explore and enjoy the planned activities."));
        }

        LOGGER.info("Using fallback parsing - Title: '{}', {} days", fallbackTitle, tripDays);
        return new RouteText(fallbackTitle, dayDescriptions);
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
            case "adventurous", "bold", "exploratory", "curious" ->
                String.format("%s Discovery Quest", formattedCity);
            case "contemplative", "thoughtful", "reflective", "nostalgic", "sad" ->
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

    private static String extractMoodFromResponse(String response) {
        String lowerResponse = response.toLowerCase();

        if (lowerResponse.contains("joyful") || lowerResponse.contains("happy")) {
            return "Joyful";
        } else if (lowerResponse.contains("relaxed") || lowerResponse.contains("calm")) {
            return "Relaxed";
        } else if (lowerResponse.contains("adventurous") || lowerResponse.contains("exciting")) {
            return "Adventurous";
        } else {
            return "Travel";
        }
    }
}
