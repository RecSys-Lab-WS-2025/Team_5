package de.tum.moodtrip_backend.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.core.model.UserProfile;
import de.tum.moodtrip_backend.infrastructure.persistence.entity.UserEntity;

@Component
public class UserMapper {

    public UserEntity toEntity(UserProfile domain) {
        if (domain == null) return null;

        UserEntity e = new UserEntity();
        e.setId(domain.id());
        e.setUsername(domain.username());
        e.setEmail(domain.email());
        e.setCreatedAt(domain.createdAt());
        e.setPasswordHash(domain.passwordHash());
        e.setSpotifyTokenId(domain.spotifyTokenId());
        e.setAvatarUrl(domain.avatarUrl());
        return e;
    }

    public UserProfile toDomain(UserEntity entity) {
        if (entity == null) return null;

        return new UserProfile(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getPasswordHash(),
                entity.getSpotifyTokenId(),
                entity.getAvatarUrl()
        );
    }
}
