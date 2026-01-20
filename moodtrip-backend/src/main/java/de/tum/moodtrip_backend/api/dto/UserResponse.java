package de.tum.moodtrip_backend.api.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt,
        Long spotifyTokenId,
        String avatarUrl
) {}
