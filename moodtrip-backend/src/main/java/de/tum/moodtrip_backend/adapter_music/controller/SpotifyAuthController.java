package de.tum.moodtrip_backend.adapter_music.controller;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.tum.moodtrip_backend.adapter_music.service.AuthService;
import de.tum.moodtrip_backend.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/spotify")
@Tag(name = "Spotify Authentication", description = "OAuth authentication endpoints for Spotify integration")
public class SpotifyAuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public SpotifyAuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @Operation(
        summary = "Initiate Spotify OAuth login",
        description = "Redirects user to Spotify authorization page to grant access permissions"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "302",
            description = "Redirect to Spotify authorization page"
        )
    })
    @GetMapping("/login")
    public Mono<ResponseEntity<String>> login() {
        String state = UUID.randomUUID().toString();
        String authorizeUrl = authService.buildAuthorizeUrl(state);
        return Mono.just( ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizeUrl))
                .build()
        );
    }

    @Operation(
        summary = "OAuth callback endpoint",
        description = "Handles the OAuth callback from Spotify, exchanges authorization code for access token, and creates/links user account"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "302",
            description = "Redirect to frontend with authentication result"
        )
    })
    @GetMapping("/callback")
    public Mono<ResponseEntity<Object>> callback(
            @Parameter(description = "Authorization code from Spotify", required = true) @RequestParam String code,
            @Parameter(description = "State parameter for CSRF protection") @RequestParam(required = false) String state,
            @Parameter(description = "Error message from Spotify") @RequestParam(required = false) String error
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

                                    String redirectUrl = String.format(
                                        "%s/chat?auth=success&token=%s&userId=%d&username=%s&email=%s",
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

    @Operation(
        summary = "Check Spotify token status",
        description = "Test endpoint to verify if a user has a valid Spotify access token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Valid token found, returns user's Spotify profile information"
        )
    })
    @GetMapping("/status")
    public Mono<ResponseEntity<String>> checkStatus(
            @Parameter(description = "User ID to check", required = true) @RequestParam Long userId) {
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