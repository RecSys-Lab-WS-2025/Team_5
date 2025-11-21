package de.tum.moodtrip_backend.core.model;

public record SpotifyTokenDomain(
        Long id,
        String accessToken,
        String refreshToken,
        Long expiresIn,
        Long fetchedAt,
        String spotifyUserId,
        String spotifyEmail,
        String spotifyDisplayName
) {
}
