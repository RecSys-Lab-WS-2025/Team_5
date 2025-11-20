package de.tum.moodtrip_backend.adapter_database.repository;

import de.tum.moodtrip_backend.adapter_database.entity.SpotifyTokenEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface R2dbcSpotifyTokenRepository extends ReactiveCrudRepository<SpotifyTokenEntity, Long> {
    Flux<SpotifyTokenEntity> findByUserIdOrderByFetchedAtDesc(Long userId);
}
