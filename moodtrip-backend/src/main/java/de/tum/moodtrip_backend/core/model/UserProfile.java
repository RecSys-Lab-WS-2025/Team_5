package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

public record UserProfile(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt,
        String passwordHash
) {
    public UserProfile withId(Long id) {
        return new UserProfile(id, username, email, createdAt, passwordHash);
    }
}
