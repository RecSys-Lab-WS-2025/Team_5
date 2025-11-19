package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

public record RouteRecommendationDomain(
    Long id,
    Long conversationId,
    JsonNode routeData,
    LocalDateTime createdAt
) {
    public RouteRecommendationDomain withId(Long id) {
        return new RouteRecommendationDomain(id, conversationId, routeData, createdAt);
    }
}
