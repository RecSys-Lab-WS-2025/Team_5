package de.tum.moodtrip_backend.infrastructure.persistence.adapter;

import de.tum.moodtrip_backend.infrastructure.persistence.mapper.SpotifyTokenMapper;
import de.tum.moodtrip_backend.infrastructure.persistence.repository.R2dbcSpotifyTokenRepository;
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
    public Mono<SpotifyTokenDomain> findBySpotifyUserId(String spotifyUserId) {
        return spotifyTokenRepository.findBySpotifyUserId(spotifyUserId)
                .map(spotifyTokenMapper::toDomain);
    }

    @Override
    public Flux<SpotifyTokenDomain> findAll() {
        return spotifyTokenRepository.findAll()
                .map(spotifyTokenMapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return spotifyTokenRepository.deleteById(id);
    }
}
