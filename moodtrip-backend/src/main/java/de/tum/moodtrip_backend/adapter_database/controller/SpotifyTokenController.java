package de.tum.moodtrip_backend.adapter_database.controller;


import de.tum.moodtrip_backend.core.model.SpotifyTokenDomain;
import de.tum.moodtrip_backend.core.port.SpotifyTokenPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/token")
public class SpotifyTokenController {
    private final SpotifyTokenPort SpotifyTokenPort;

    public SpotifyTokenController(SpotifyTokenPort spotifyTokenPort) {
        this.SpotifyTokenPort = spotifyTokenPort;
    }

    @PostMapping
    public Mono<SpotifyTokenDomain> create(@RequestBody CreateSpotifyTokenRequest request) {
        if (request.userId == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "UserId cannot be null"
            ));
        }

        SpotifyTokenDomain domain = new SpotifyTokenDomain(
                null,
                request.userId,
                request.accessToken,
                request.refreshToken,
                request.expiresIn,
                System.currentTimeMillis() / 1000
        );
        return SpotifyTokenPort.save(domain);
    }

    @GetMapping("/{userId}")
    public Mono<SpotifyTokenDomain> getByUserId(@PathVariable Long userId) {
        return SpotifyTokenPort.findByUserId(userId)
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spotify token not found for userId: " + userId
                )));
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteById(@PathVariable Long id) {
        return SpotifyTokenPort.deleteById(id);
    }


    public record CreateSpotifyTokenRequest(Long userId,
                                            String accessToken, String refreshToken,
                                            Long expiresIn) {
    }
}
