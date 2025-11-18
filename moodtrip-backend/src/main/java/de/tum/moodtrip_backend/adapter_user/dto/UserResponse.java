package de.tum.moodtrip_backend.adapter_user.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt
) {}
