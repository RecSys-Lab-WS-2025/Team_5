package de.tum.moodtrip_backend.adapter.content.chatbot.mapper;

import de.tum.moodtrip_backend.core.model.RouteText;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteTextMapperTest {



    @Test
    void testParseBatchResponse() {
        String jsonResponse = "{" +
                "\"BALANCED\": {\"title\": \"Balanced Trip\", \"dayDescriptions\": {\"1\": \"Day 1 desc\"}}," +
                "\"YOUR_PICKS\": {\"title\": \"Your Picks Trip\", \"dayDescriptions\": {\"1\": \"Day 1 desc\"}}," +
                "\"DISCOVERY\": {\"title\": \"Discovery Trip\", \"dayDescriptions\": {\"1\": \"Day 1 desc\"}}" +
                "}";

        Map<de.tum.moodtrip_backend.core.model.RouteType, RouteText> result = RouteTextMapper.parseBatchResponse(jsonResponse);

        assertEquals(3, result.size());
        assertEquals("Balanced Trip", result.get(de.tum.moodtrip_backend.core.model.RouteType.BALANCED).title());
        assertEquals("Your Picks Trip", result.get(de.tum.moodtrip_backend.core.model.RouteType.YOUR_PICKS).title());
        assertEquals("Discovery Trip", result.get(de.tum.moodtrip_backend.core.model.RouteType.DISCOVERY).title());
    }
}
