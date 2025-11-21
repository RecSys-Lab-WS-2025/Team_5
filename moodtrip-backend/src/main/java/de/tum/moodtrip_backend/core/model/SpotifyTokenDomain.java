package de.tum.moodtrip_backend.core.model;

public record SpotifyTokenDomain(
        Long id,
        Long userId,
        String accessToken,
        String refreshToken,
        Long expiresIn,
        Long fetchedAt
) {
}
