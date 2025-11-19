package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

public record MusicRecommendationDomain(
    Long id,
    Long conversationId,
    String title,
    String link,
    LocalDateTime createdAt
) {
    public MusicRecommendationDomain withId(Long id) {
        return new MusicRecommendationDomain(id, conversationId, title, link, createdAt);
    }
}
