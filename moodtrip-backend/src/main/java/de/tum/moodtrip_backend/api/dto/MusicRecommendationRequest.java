package de.tum.moodtrip_backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MusicRecommendationRequest(
        @NotBlank(message = "emotion can not be blank") String emotion,
        @NotNull(message = "ConvId cannot be null") Long conversationId
) {
}
