package de.tum.moodtrip_backend.adapter_user.dto;

import jakarta.validation.constraints.NotBlank;

public record ConversationCreateRequest(
                                        Long userId,
                                        @NotBlank
                                        String title) {
}
