package de.tum.moodtrip_backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MessageContentRequest(
        @NotBlank(message = "Content cannot be blank") String content,
        @NotNull boolean isUser
) {
}
