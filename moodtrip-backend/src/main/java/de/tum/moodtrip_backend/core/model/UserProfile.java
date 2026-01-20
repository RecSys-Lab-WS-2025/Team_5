package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

public record UserProfile(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt,
        String passwordHash,
        Long spotifyTokenId,
        String avatarUrl
) {

    public UserProfile withId(Long id) {
        return new UserProfile(
                id,
                username,
                email,
                createdAt,
                passwordHash,
                spotifyTokenId,
                avatarUrl
        );
    }

    public UserProfile withUsername(String username) {
        return new UserProfile(
                id,
                username,
                email,
                createdAt,
                passwordHash,
                spotifyTokenId,
                avatarUrl
        );
    }

    public UserProfile withAvatarUrl(String avatarUrl) {
        return new UserProfile(
                id,
                username,
                email,
                createdAt,
                passwordHash,
                spotifyTokenId,
                avatarUrl
        );
    }

    public boolean hasSpotifyAuthorization() {
        return spotifyTokenId != null;
    }
}
