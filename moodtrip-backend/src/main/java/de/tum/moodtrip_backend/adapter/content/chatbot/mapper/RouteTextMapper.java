package de.tum.moodtrip_backend.adapter.content.chatbot.mapper;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tum.moodtrip_backend.core.model.EnrichedPoi;
import de.tum.moodtrip_backend.core.model.RouteText;

/**
 * Mapper for parsing AI-generated route text responses into RouteText objects.
 * Supports multiple response formats: JSON, line-based, and fallback parsing.
 */
public class RouteTextMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteTextMapper.class);

    /**
     * Parse AI response string into RouteText
     * Supports JSON format, line format, and fallback parsing
     */
    public static RouteText fromAiResponse(String response) {
        LOGGER.info("Parsing AI response into RouteText, response length: {} chars", response.length());
        
        String cleanedResponse = response.trim();
        
        // Try JSON format first
        RouteText jsonResult = tryParseJsonFormat(cleanedResponse);
        if (jsonResult != null) {
            return jsonResult;
        }
        
        // Try line format
        RouteText lineResult = tryParseLineFormat(cleanedResponse);
        if (lineResult != null) {
            return lineResult;
        }
        
        // Fallback: use entire response as description
        return parseFallback(cleanedResponse);
    }

    /**
     * Generate mock RouteText for testing (bypasses AI)
     */
    public static RouteText createMockRouteText(String mood, String city, List<EnrichedPoi> pois) {
        LOGGER.debug("Creating mock route text for mood: {}, city: {}", mood, city);
        
        String title = generateMockTitle(mood, city);
        String description = generateMockDescription(mood, city, pois);
        
        LOGGER.info("Generated mock route - Title: '{}', Description length: {} chars", 
            title, description.length());
        
        return new RouteText(title, description);
    }

    /**
     * Create error RouteText for invalid inputs
     */
    public static RouteText createErrorRouteText(String errorMessage, String city) {
        String title = "Error Route";
        String description = String.format(
            "Unable to generate route: %s. This is a mock response for error handling demonstration in %s.", 
            errorMessage, 
            city != null ? city : "unknown location"
        );
        
        LOGGER.warn("Creating error route text: {}", errorMessage);
        return new RouteText(title, description);
    }

    // ==================== Private Parsing Methods ====================

    /**
     * Attempt to parse JSON format: {"title": "...", "description": "..."}
     * Uses proper JSON parsing to handle commas in description
     */
    private static RouteText tryParseJsonFormat(String response) {
        if (!response.startsWith("{") || !response.endsWith("}")) {
            LOGGER.debug("Not JSON format (no braces)");
            return null;
        }

        try {
            // Try to find title and description using regex to handle commas in values
            String titlePattern = "\"title\"\\s*:\\s*\"([^\"]*)\"|'title'\\s*:\\s*'([^']*)'";
            String descPattern = "\"description\"\\s*:\\s*\"(.*?)\"(?=\\s*[,}])|'description'\\s*:\\s*'(.*?)'(?=\\s*[,}])";
            
            java.util.regex.Pattern titleRe = java.util.regex.Pattern.compile(titlePattern);
            java.util.regex.Matcher titleMatcher = titleRe.matcher(response);
            
            java.util.regex.Pattern descRe = java.util.regex.Pattern.compile(descPattern);
            java.util.regex.Matcher descMatcher = descRe.matcher(response);
            
            String extractedTitle = null;
            String extractedDescription = null;
            
            if (titleMatcher.find()) {
                extractedTitle = titleMatcher.group(1) != null ? titleMatcher.group(1) : titleMatcher.group(2);
            }
            
            if (descMatcher.find()) {
                extractedDescription = descMatcher.group(1) != null ? descMatcher.group(1) : descMatcher.group(2);
            }
            
            if (extractedTitle != null && extractedDescription != null) {
                LOGGER.info("Successfully parsed JSON format - Title: '{}', Description length: {} chars", 
                    extractedTitle, extractedDescription.length());
                return new RouteText(extractedTitle, extractedDescription);
            } else {
                LOGGER.warn("JSON format detected but fields not found - title: {}, description: {}", 
                    extractedTitle != null, extractedDescription != null);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse as JSON format: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Attempt to parse line format:
     * Title: ...
     * Description: ...
     */
    private static RouteText tryParseLineFormat(String response) {
        try {
            String[] lines = response.split("\n");
            String title = null;
            String description = null;
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.toLowerCase().startsWith("title:")) {
                    title = trimmedLine.substring(6).trim().replaceAll("^\"|\"$", "");
                } else if (trimmedLine.toLowerCase().startsWith("description:")) {
                    description = trimmedLine.substring(12).trim().replaceAll("^\"|\"$", "");
                }
            }
            
            if (title != null && description != null) {
                LOGGER.info("Successfully parsed line format");
                return new RouteText(title, description);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse line format: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Fallback parsing: use entire response as description
     */
    private static RouteText parseFallback(String response) {
        String fallbackTitle = "Personalized " + extractMoodFromResponse(response) + " Journey";
        String fallbackDescription = response;
        
        LOGGER.info("Using fallback parsing - Title: '{}'", fallbackTitle);
        return new RouteText(fallbackTitle, fallbackDescription);
    }

    // ==================== Mock Generation Methods ====================

    /**
     * Generate varied mock titles based on mood and city
     */
    private static String generateMockTitle(String mood, String city) {
        String formattedMood = capitalizeFirst(mood);
        String formattedCity = city != null && !city.trim().isEmpty() ? city.trim() : "Test Location";
    
        return switch (mood.toLowerCase()) {
            case "joyful", "happy", "excited" -> 
                String.format("%s %s Adventure", formattedMood, formattedCity);
            case "relaxed", "calm", "peaceful" -> 
                String.format("Serene %s Escape", formattedCity);
            case "adventurous", "bold", "exploratory" -> 
                String.format("%s Discovery Quest", formattedCity);
            case "contemplative", "thoughtful", "reflective" -> 
                String.format("Mindful %s Journey", formattedCity);
            default -> 
                String.format("Mock %s Route in %s", formattedMood, formattedCity);
        };
    }

    /**
     * Generate detailed mock descriptions with specific POI mentions
     */
    private static String generateMockDescription(String mood, String city, List<EnrichedPoi> pois) {
        String formattedCity = city != null && !city.trim().isEmpty() ? city.trim() : "the test location";
        
        // If we have POIs, create a detailed description mentioning each one
        if (pois != null && !pois.isEmpty()) {
            StringBuilder description = new StringBuilder();
            
            // Intro based on mood
            String intro = switch (mood.toLowerCase()) {
                case "joyful", "happy" -> 
                    String.format("Begin your joyful journey through %s, ", formattedCity);
                case "relaxed", "calm" -> 
                    String.format("Start your serene exploration of %s, ", formattedCity);
                case "adventurous", "bold" -> 
                    String.format("Launch your exciting adventure across %s, ", formattedCity);
                case "contemplative", "thoughtful" -> 
                    String.format("Embark on a mindful journey through %s, ", formattedCity);
                default -> 
                    String.format("Discover %s through this carefully curated route, ", formattedCity);
            };
            
            description.append(intro);
            
            // Mention each POI with connection words
            int poiLimit = Math.min(pois.size(), 5);
            for (int i = 0; i < poiLimit; i++) {
                EnrichedPoi poi = pois.get(i);
                String poiName = poi.poi().name();
                String poiDesc = poi.description();
                
                // Connection words based on position
                String connector = switch (i) {
                    case 0 -> "visiting ";
                    case 1 -> "then exploring ";
                    case 2 -> "followed by ";
                    default -> i == poiLimit - 1 ? "and concluding at " : "continuing to ";
                };
                
                description.append(connector).append(poiName);
                
                // Add brief description if available
                if (poiDesc != null && !poiDesc.trim().isEmpty()) {
                    // Truncate long descriptions
                    String shortDesc = poiDesc.length() > 80 ? 
                        poiDesc.substring(0, 77) + "..." : poiDesc;
                    description.append(" - ").append(shortDesc);
                }
                
                // Add punctuation
                if (i < poiLimit - 1) {
                    description.append(", ");
                } else {
                    description.append(".");
                }
            }
            
            return description.toString();
        }
        
        // Fallback if no POIs (original behavior)
        String baseTemplate = switch (mood.toLowerCase()) {
            case "joyful", "happy" -> 
                "Experience pure joy through %s's finest attractions. This curated journey combines carefully selected destinations designed to uplift your spirits and create lasting memories.";
            case "relaxed", "calm" -> 
                "Unwind in %s's most tranquil settings. This peaceful itinerary features serene locations perfect for rest, reflection, and gentle exploration away from the hustle and bustle.";
            case "adventurous", "bold" -> 
                "Embark on an exciting adventure through %s's dynamic landscape. This bold expedition includes thrilling destinations that promise discovery, challenge, and unforgettable experiences.";
            case "contemplative", "thoughtful" -> 
                "Journey mindfully through %s's culturally rich environment. This thoughtful route connects meaningful locations that inspire reflection and deeper appreciation for your surroundings.";
            default -> 
                "This is a comprehensive mock route in %s for testing purposes. The carefully planned journey includes key locations designed to verify proper functionality, user experience, and system integration.";
        };
    
        String testingContext = " Features include automated testing protocols, system validation checkpoints, and quality assurance measures to ensure optimal performance.";
    
        return String.format(baseTemplate + testingContext, formattedCity);
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
