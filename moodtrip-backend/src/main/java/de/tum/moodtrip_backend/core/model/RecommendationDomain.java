package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

public record RecommendationDomain(
    Long id,
    Long conversationId,
    String type,
    String title,
    String description,
    String link,
    String trackId,
    String routeData,
    LocalDateTime createdAt
) {
    public RecommendationDomain withId(Long id) {
        return new RecommendationDomain(id, conversationId, type, title, description, link, trackId, routeData, createdAt);
    }
}
