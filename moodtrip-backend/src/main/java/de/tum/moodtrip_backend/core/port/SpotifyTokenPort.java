package de.tum.moodtrip_backend.core.port;
import de.tum.moodtrip_backend.core.model.SpotifyTokenDomain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SpotifyTokenPort {
    Mono<SpotifyTokenDomain> save(SpotifyTokenDomain spotifyTokenDomain);
    Mono<SpotifyTokenDomain> findById(Long id);
    Mono<SpotifyTokenDomain> findBySpotifyUserId(String spotifyUserId);
    Flux<SpotifyTokenDomain> findAll();
    Mono<Void> deleteById(Long id);
}
