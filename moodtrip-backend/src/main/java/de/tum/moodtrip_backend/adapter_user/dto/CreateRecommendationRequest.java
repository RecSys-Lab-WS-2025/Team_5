package de.tum.moodtrip_backend.adapter_user.dto;

import jakarta.validation.constraints.NotNull;

public record CreateRecommendationRequest(
        @NotNull Long conversationId,
        @NotNull String type,
        @NotNull String title,
        String description,
        String link,
        String trackId,
        String routeData
) {}