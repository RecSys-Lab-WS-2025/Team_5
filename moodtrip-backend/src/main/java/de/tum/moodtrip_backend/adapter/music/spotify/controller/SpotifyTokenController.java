package de.tum.moodtrip_backend.adapter.music.spotify.controller;


import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.adapter.music.spotify.service.SpotifyTokenService;
import de.tum.moodtrip_backend.api.security.JwtService;
import de.tum.moodtrip_backend.core.model.SpotifyTokenDomain;
import reactor.core.publisher.Mono;


@RestController

@RequestMapping("/api/token")
public class SpotifyTokenController {

    private final JwtService jwtService;
    private final SpotifyTokenService spotifyTokenService;

    public SpotifyTokenController(JwtService jwtService, SpotifyTokenService spotifyTokenService) {
        this.jwtService = jwtService;
        this.spotifyTokenService = spotifyTokenService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
        return spotifyTokenService.create(domain);
    }

    @GetMapping("/{id}")
    public Mono<SpotifyTokenDomain> getById(
            @PathVariable Long id,
            Authentication authentication) {


        jwtService.extractUserId(authentication);

        return spotifyTokenService.getById(id);
    }

    @GetMapping("/spotify-user/{spotifyUserId}")
    public Mono<SpotifyTokenDomain> getBySpotifyUserId(
            @PathVariable String spotifyUserId,
            Authentication authentication) {


        jwtService.extractUserId(authentication);

        return spotifyTokenService.getBySpotifyUserId(spotifyUserId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteById(
            @PathVariable Long id,
            Authentication authentication) {
        long authUserId = jwtService.extractUserId(authentication);

        return spotifyTokenService.deleteById(id, authUserId);
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
