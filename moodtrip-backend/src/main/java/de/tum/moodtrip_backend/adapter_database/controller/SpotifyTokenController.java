package de.tum.moodtrip_backend.adapter_database.controller;


import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.core.model.SpotifyTokenDomain;
import de.tum.moodtrip_backend.core.port.SpotifyTokenPort;
import de.tum.moodtrip_backend.security.JwtService;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/token")
public class SpotifyTokenController {
    private final SpotifyTokenPort SpotifyTokenPort;
    private final JwtService jwtService;

    public SpotifyTokenController(SpotifyTokenPort spotifyTokenPort, JwtService jwtService) {
        this.SpotifyTokenPort = spotifyTokenPort;
        this.jwtService = jwtService;
    }

    @PostMapping
    public Mono<SpotifyTokenDomain> create(
            @RequestBody CreateSpotifyTokenRequest request,
            Authentication authentication) {
        
       
        jwtService.extractUserId(authentication);
        
        if (request.accessToken == null || request.accessToken.isBlank()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AccessToken cannot be null or empty"
            ));
        }

        SpotifyTokenDomain domain = new SpotifyTokenDomain(
                null,
                request.accessToken,
                request.refreshToken,
                request.expiresIn,
                System.currentTimeMillis() / 1000,
                request.spotifyUserId,
                request.spotifyEmail,
                request.spotifyDisplayName
        );
        return SpotifyTokenPort.save(domain);
    }

    @GetMapping("/{id}")
    public Mono<SpotifyTokenDomain> getById(
            @PathVariable Long id,
            Authentication authentication) {
        
        
        jwtService.extractUserId(authentication);
        
        return SpotifyTokenPort.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spotify token not found for id: " + id
                )));
    }

    @GetMapping("/spotify-user/{spotifyUserId}")
    public Mono<SpotifyTokenDomain> getBySpotifyUserId(
            @PathVariable String spotifyUserId,
            Authentication authentication) {
        
        
        jwtService.extractUserId(authentication);
        
        return SpotifyTokenPort.findBySpotifyUserId(spotifyUserId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spotify token not found for spotifyUserId: " + spotifyUserId
                )));
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteById(
            @PathVariable Long id,
            Authentication authentication) {
        
        
        jwtService.extractUserId(authentication);
        
        return SpotifyTokenPort.deleteById(id);
    }


    public record CreateSpotifyTokenRequest(
            String accessToken,
            String refreshToken,
            Long expiresIn,
            String spotifyUserId,
            String spotifyEmail,
            String spotifyDisplayName
    ) {
    }
}
