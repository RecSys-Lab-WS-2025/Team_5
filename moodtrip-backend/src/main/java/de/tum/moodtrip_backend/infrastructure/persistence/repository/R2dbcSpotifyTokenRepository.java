package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.SpotifyTokenEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface R2dbcSpotifyTokenRepository extends ReactiveCrudRepository<SpotifyTokenEntity, Long> {
    Mono<SpotifyTokenEntity> findBySpotifyUserId(String spotifyUserId);
}
