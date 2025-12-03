package de.tum.moodtrip_backend.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ConversationCreateRequest(
                                        Long userId,
                                        @NotBlank
                                        String title) {
}
