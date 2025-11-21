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
                entity.getUserId(),
                entity.getAccessToken(),
                entity.getRefreshToken(),
                entity.getExpiresIn(),
                entity.getFetchedAt()
        );
    }

    public SpotifyTokenEntity toEntity(SpotifyTokenDomain domain) {
        if (domain == null) {
            return null;
        }
        return new SpotifyTokenEntity(
                domain.id(),
                domain.userId(),
                domain.accessToken(),
                domain.refreshToken(),
                domain.expiresIn(),
                domain.fetchedAt()
        );
    }
}
