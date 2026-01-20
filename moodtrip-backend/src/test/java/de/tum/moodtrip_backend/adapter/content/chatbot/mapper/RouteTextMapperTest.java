package de.tum.moodtrip_backend.adapter.content.chatbot.mapper;

import de.tum.moodtrip_backend.core.model.RouteText;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteTextMapperTest {

    @Test
    void testFromAiResponse_ValidJson() {
        String jsonResponse = "{" +
                "\"title\": \"Happy Day in Munich\"," +
                "\"dayDescriptions\": {" +
                    "\"1\": \"Visit Marienplatz.\"," +
                    "\"2\": \"Go to English Garden.\"" +
                "}" +
                "}";

        RouteText result = RouteTextMapper.fromAiResponse(jsonResponse, 2);

        assertNotNull(result);
        assertEquals("Happy Day in Munich", result.title());
        assertEquals(2, result.dayDescriptions().size());
        assertEquals("Visit Marienplatz.", result.dayDescriptions().get(1));
        assertEquals("Go to English Garden.", result.dayDescriptions().get(2));
    }

    @Test
    void testFromAiResponse_ValidJsonWithSingleQuotes() {
        // Our objectMapper configuration allows single quotes
        String jsonResponse = "{" +
                "'title': 'Relaxed Day'," +
                "'dayDescriptions': {" +
                    "'1': 'Chilling.'" +
                "}" +
                "}";

        RouteText result = RouteTextMapper.fromAiResponse(jsonResponse, 1);

        assertNotNull(result);
        assertEquals("Relaxed Day", result.title());
        assertEquals(1, result.dayDescriptions().size());
        assertEquals("Chilling.", result.dayDescriptions().get(1));
    }

    @Test
    void testFromAiResponse_InvalidJson_Fallback() {
        String rawText = "Just a normal text response without JSON.";

        RouteText result = RouteTextMapper.fromAiResponse(rawText, 1);

        assertNotNull(result);
        // Fallback title usually starts with "Personalized"
        assertTrue(result.title().startsWith("Personalized")); 
        assertEquals(1, result.dayDescriptions().size());
        assertTrue(result.dayDescriptions().get(1).contains("Just a normal text response"));
    }

    @Test
    void testFromAiResponse_JsonMissingFields_Fallback() {
        String jsonResponse = "{\"foo\": \"bar\"}";

        RouteText result = RouteTextMapper.fromAiResponse(jsonResponse, 1);

        assertNotNull(result);
        // Should fallback
        assertTrue(result.title().startsWith("Personalized"));
    }
}
