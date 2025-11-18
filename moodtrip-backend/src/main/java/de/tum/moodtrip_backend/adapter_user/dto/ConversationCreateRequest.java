package de.tum.moodtrip_backend.adapter_user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ConversationCreateRequest(@NotBlank
                                        long userId,
                                        @NotBlank @Email
                                        String title) {
}
