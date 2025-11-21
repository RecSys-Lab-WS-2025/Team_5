package de.tum.moodtrip_backend.adapter_music.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.adapter_music.service.AuthService;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyAuthController {

    private final AuthService authService;
    private static final String FRONTEND_URL = "http://localhost:5173";

    public SpotifyAuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Redirect user to Spotify authorization page
     */
    @GetMapping("/login")
    public Mono<ResponseEntity<String>> login(@RequestParam(required = true) Long userId) {
        if (userId == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Missing required parameter: userId\"}"));
        }
        String state = String.valueOf(userId);
        String authorizeUrl = authService.buildAuthorizeUrl(state);

        return Mono.just( ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizeUrl))
                .build()
        );
    }

    /**
     * OAuth callback endpoint - Spotify redirects here after user authorization
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Object>> callback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        // 1. 处理 Spotify 返回的错误
        if (error != null) {
            return redirectWithError("spotify_error: " + error);
        }

        // 2. 校验 State
        if (state == null || state.isEmpty()) {
            return redirectWithError("missing_state");
        }

        long userIdFromState;
        try {
            userIdFromState = Long.parseLong(state);
        } catch (NumberFormatException e) {
            return redirectWithError("invalid_state_format");
        }
        final long userId = userIdFromState;

        return authService.exchangeCodeForToken(code, userId)
                .map(tokenDomain ->
                        ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(FRONTEND_URL + "?spotify=success"))
                            .build()
                )
                .onErrorResume(e ->
                        redirectWithError("token_exchange_failed")
                );
    }


    private Mono<ResponseEntity<Object>> redirectWithError(String errorMessage) {
        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(FRONTEND_URL + "?spotify=error&msg=" + encodedError))
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