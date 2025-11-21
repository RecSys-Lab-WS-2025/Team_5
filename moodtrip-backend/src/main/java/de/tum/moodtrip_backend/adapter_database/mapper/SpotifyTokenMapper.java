package de.tum.moodtrip_backend.adapter_database.mapper;

import de.tum.moodtrip_backend.core.model.SpotifyTokenDomain;
import de.tum.moodtrip_backend.adapter_database.entity.SpotifyTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class SpotifyTokenMapper {
    public SpotifyTokenDomain toDomain(SpotifyTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SpotifyTokenDomain(
                entity.getId(),
                entity.getAccessToken(),
                entity.getRefreshToken(),
                entity.getExpiresIn(),
                entity.getFetchedAt(),
                entity.getSpotifyUserId(),
                entity.getSpotifyEmail(),
                entity.getSpotifyDisplayName()
        );
    }

    public SpotifyTokenEntity toEntity(SpotifyTokenDomain domain) {
        if (domain == null) {
            return null;
        }
        return new SpotifyTokenEntity(
                domain.id(),
                domain.accessToken(),
                domain.refreshToken(),
                domain.expiresIn(),
                domain.fetchedAt(),
                domain.spotifyUserId(),
                domain.spotifyEmail(),
                domain.spotifyDisplayName()
        );
    }
}
