package de.tum.moodtrip_backend.adapter_music.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.adapter_music.service.AuthService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/spotify")
public class SpotifyAuthController {

    private final AuthService authService;

    public SpotifyAuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Redirect user to Spotify authorization page
     */
    @GetMapping("/authorize")
    public Mono<ResponseEntity<String>> authorize(@RequestParam(required = true) Long userId) {
        if (userId == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Missing required parameter: userId\"}"));
        }
        String state = String.valueOf(userId);
        String authorizeUrl = authService.buildAuthorizeUrl(state);

        return Mono.just(ResponseEntity.ok(
                "{\"authUrl\":\"" + authorizeUrl + "\"}"
        ));
    }

    /**
     * OAuth callback endpoint - Spotify redirects here after user authorization
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<String>> callback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        if (error != null) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Authorization failed: " + error));
        }

        if (state == null || state.isEmpty()) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Missing required state parameter"));
        }
        long userIdFromState;
        try {
            userIdFromState = Long.parseLong(state);
        } catch (NumberFormatException e) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Invalid state parameter"));
        }
        final long userId = userIdFromState;

        return authService.exchangeCodeForToken(code, userId)
                .flatMap(spotifyToken ->
                        authService.getCurrentUserProfile(spotifyToken.accessToken())
                                .map(profile -> ResponseEntity.ok(
                                        "✅ Authorization successful!\n" +
                                                "User: " + profile.path("display_name").asText() + "\n" +
                                                "Email: " + profile.path("email").asText() + "\n" +
                                                "Token saved for userId: " + userId
                                ))
                )
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("❌ Token exchange failed: " + e.getMessage())
                ));
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