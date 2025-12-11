package de.tum.moodtrip_backend.adapter.music.spotify.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.adapter.music.spotify.service.AuthService;
import de.tum.moodtrip_backend.api.security.JwtService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyAuthController {

    private static final Logger log = LoggerFactory.getLogger(SpotifyAuthController.class);
    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public SpotifyAuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    /**
     * Redirect user to Spotify authorization page
     */
    @GetMapping("/login")
    public Mono<ResponseEntity<String>> login() {
        String state = UUID.randomUUID().toString();
        String authorizeUrl = authService.buildAuthorizeUrl(state);
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizeUrl))
                .build()
        );
    }

    /**
     * OAuth callback endpoint - Spotify redirects here after user authorization
     * AuthService handles both SpotifyToken and UserProfile creation/linking
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Object>> callback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        // 1. Handle errors returned by Spotify
        if (error != null) {
            return redirectWithError("spotify_error: " + error);
        }

        // 2. Validate state parameter for CSRF protection
        if (state == null || state.isEmpty()) {
            return redirectWithError("missing_state");
        }


        return authService.exchangeCodeForToken(code)
                .flatMap(spotifyToken ->
                        // Get the linked user profile through AuthService
                        authService.getUserBySpotifyTokenId(spotifyToken.id())
                                .map(user -> {
                                    String jwtToken = jwtService.generateToken(user);

                                    String encodedUsername = URLEncoder.encode(user.username(), StandardCharsets.UTF_8);
                                    String encodedEmail = URLEncoder.encode(user.email() != null ? user.email() : "", StandardCharsets.UTF_8);
                                    log.info("jwt Token {}", jwtToken);

                                    String redirectUrl = String.format(
                                            "%s/?auth=success&token=%s&userId=%d&username=%s&email=%s",
                                            frontendUrl, jwtToken, user.id(), encodedUsername, encodedEmail
                                    );

                                    return ResponseEntity.status(HttpStatus.FOUND)
                                            .location(URI.create(redirectUrl))
                                            .build();
                                })
                )
                .onErrorResume(e ->
                        redirectWithError("login_failed: " + e.getMessage())
                );
    }


    private Mono<ResponseEntity<Object>> redirectWithError(String errorMessage) {
        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "?auth=error&msg=" + encodedError))
                .build());
    }

    /**
     * Test endpoint to check if user has valid token
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<String>> checkStatus(@RequestParam Long userId) {
        return authService.getAccessToken(userId)
                .flatMap(token ->
                        authService.getCurrentUserProfile(token)
                                .map(profile -> ResponseEntity.ok(
                                        "✅ User " + userId + " has valid token\n" +
                                                "Spotify User: " + profile.path("display_name").asText()
                                ))
                )
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("❌ No valid token for user " + userId + ": " + e.getMessage())
                ));
    }
}