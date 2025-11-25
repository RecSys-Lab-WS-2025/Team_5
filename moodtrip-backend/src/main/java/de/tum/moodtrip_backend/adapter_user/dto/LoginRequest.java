package de.tum.moodtrip_backend.adapter_user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email cannot be blank") @Email(message = "Email should be valid") String email,
        @NotBlank(message = "Password cannot be blank") String password
) {
}
