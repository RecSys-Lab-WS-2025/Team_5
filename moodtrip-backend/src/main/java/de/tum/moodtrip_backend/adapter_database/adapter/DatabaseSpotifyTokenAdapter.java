package de.tum.moodtrip_backend.adapter_database.adapter;

import de.tum.moodtrip_backend.adapter_database.mapper.SpotifyTokenMapper;
import de.tum.moodtrip_backend.adapter_database.repository.R2dbcSpotifyTokenRepository;
import de.tum.moodtrip_backend.core.model.SpotifyTokenDomain;
import de.tum.moodtrip_backend.core.port.SpotifyTokenPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Component
public class DatabaseSpotifyTokenAdapter implements SpotifyTokenPort {
    private final SpotifyTokenMapper spotifyTokenMapper;
    private final R2dbcSpotifyTokenRepository spotifyTokenRepository;

    public DatabaseSpotifyTokenAdapter(SpotifyTokenMapper spotifyTokenMapper, R2dbcSpotifyTokenRepository spotifyTokenRepository) {
        this.spotifyTokenMapper = spotifyTokenMapper;
        this.spotifyTokenRepository = spotifyTokenRepository;
    }

    @Override
    public Mono<SpotifyTokenDomain> save(SpotifyTokenDomain spotifyTokenDomain) {
        return Mono.just(spotifyTokenDomain)
                .map(spotifyTokenMapper::toEntity)
                .flatMap(spotifyTokenRepository::save)
                .map(spotifyTokenMapper::toDomain);
    }

    @Override
    public Mono<SpotifyTokenDomain> findById(Long id) {
        return spotifyTokenRepository.findById(id)
                        .map(spotifyTokenMapper::toDomain);
    }

    @Override
    public Flux<SpotifyTokenDomain> findByUserId(Long userId) {
        return spotifyTokenRepository.findByUserIdOrderByFetchedAtDesc(userId)
                .map(spotifyTokenMapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return spotifyTokenRepository.deleteById(id);
    }
}
