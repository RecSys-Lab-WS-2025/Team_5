package de.tum.moodtrip_backend.adapter_music.service;


import de.tum.moodtrip_backend.core.model.SpotifyTokenDomain;
import de.tum.moodtrip_backend.core.port.SpotifyTokenPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Service
public class AuthService {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${SPOTIFY_TOKEN:}")
    private String token;

    @Value("${spotify.redirect-uri}")
    private String redirectUri;

    @Value("${spotify.auth-url}")
    private String authBaseUrl;

    @Value("${spotify.api-base-url}")
    private String apiBaseUrl;

    @Value("${spotify.scopes}")
    private String scopes;

    private final WebClient webClientAuth;
    private final SpotifyTokenPort spotifyTokenPort;

    public AuthService(WebClient.Builder webClientBuilder, SpotifyTokenPort spotifyTokenPort) {
        this.webClientAuth = webClientBuilder.baseUrl("https://accounts.spotify.com").build();
        this.spotifyTokenPort = spotifyTokenPort;
    }

    /**
     * Get access token for a specific user ID (SpotifyToken.id), with automatic refresh if expired
     */
    public Mono<String> getAccessToken(Long userId) {
        return spotifyTokenPort.findById(userId)
                .flatMap(spotifyToken -> {
                    long currentTime = System.currentTimeMillis() / 1000;
                    long tokenAge = currentTime - spotifyToken.fetchedAt();

                    // Token expires in spotifyToken.expiresIn() seconds, refresh if less than 5 minutes left
                    if (tokenAge + 300 >= spotifyToken.expiresIn()) {
                        System.out.println("Token expired or about to expire, refreshing...");
                        return refreshAndSaveToken(spotifyToken);
                    }

                    return Mono.just(spotifyToken.accessToken());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Fallback to env token if no user token found (for backward compatibility)
                    if (token != null && !token.isBlank()) {
                        System.out.println("Using fallback env token for user " + userId);
                        return Mono.just(token);
                    }
                    return Mono.error(new IllegalStateException(
                            "No access token found for user " + userId + ". Please authorize via OAuth."
                    ));
                }));
    }

    /**
     * Get access token without user context (uses env token or first available user token)
     */
    public Mono<String> getAccessToken() {
        if (token != null && !token.isBlank()) {
            return Mono.just(token);
        }

        // Try to get any available user token
        return spotifyTokenPort.findAll()
                .next()
                .flatMap(spotifyToken -> getAccessToken(spotifyToken.id()))
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "No access token configured. Please authorize via OAuth."
                )));
    }



    /**
     * Refresh token and save to database
     */
    private Mono<String> refreshAndSaveToken(SpotifyTokenDomain existingToken) {
        return webClientAuth.post()
                .uri("/api/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", existingToken.refreshToken()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(json -> {
                    String newAccessToken = json.path("access_token").asText();
                    long expiresIn = json.path("expires_in").asLong();
                    long fetchedAt = System.currentTimeMillis() / 1000;

                    String newRefreshToken = json.path("refresh_token").asText("");
                    if (newRefreshToken.isEmpty()) {
                        newRefreshToken = existingToken.refreshToken();
                    }

                    SpotifyTokenDomain updatedToken = new SpotifyTokenDomain(
                            existingToken.id(),
                            newAccessToken,
                            newRefreshToken,
                            expiresIn,
                            fetchedAt,
                            existingToken.spotifyUserId(),
                            existingToken.spotifyEmail(),
                            existingToken.spotifyDisplayName()
                    );

                    return spotifyTokenPort.save(updatedToken)
                            .map(SpotifyTokenDomain::accessToken);
                });
    }

    /**
     * Refresh access token (public API for manual refresh)
     */
    public Mono<JsonNode> refreshAccessToken(String refreshToken) {
        return webClientAuth.post()
                .uri("/api/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", refreshToken))
                .retrieve()
                .bodyToMono(JsonNode.class);
    }


    /**
     * Exchange authorization code for tokens and save to database
     * Returns SpotifyTokenDomain where id is the userId
     */
    public Mono<SpotifyTokenDomain> exchangeCodeForToken(String code) {
        System.out.printf("Exchanging code for token, code: %s%n", code);
        return webClientAuth.post()
                .uri("/api/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("code", code)
                        .with("redirect_uri", redirectUri)
                )
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(tokenJson -> {
                    String accessToken = tokenJson.path("access_token").asText();
                    String refreshToken = tokenJson.path("refresh_token").asText();
                    long expiresIn = tokenJson.path("expires_in").asLong();
                    long fetchedAt = System.currentTimeMillis() / 1000;

                    // Get Spotify user profile to obtain spotifyUserId
                    return getCurrentUserProfile(accessToken)
                            .flatMap(profileJson -> {
                                String spotifyUserId = profileJson.path("id").asText();
                                String spotifyEmail = profileJson.path("email").asText();
                                String spotifyDisplayName = profileJson.path("display_name").asText();

                                // Check if user already exists by spotifyUserId
                                return spotifyTokenPort.findBySpotifyUserId(spotifyUserId)
                                        .flatMap(existingToken -> {
                                            // Update existing token
                                            SpotifyTokenDomain updated = new SpotifyTokenDomain(
                                                    existingToken.id(),
                                                    accessToken,
                                                    refreshToken,
                                                    expiresIn,
                                                    fetchedAt,
                                                    spotifyUserId,
                                                    spotifyEmail,
                                                    spotifyDisplayName
                                            );
                                            return spotifyTokenPort.save(updated);
                                        })
                                        .switchIfEmpty(Mono.defer(() -> {
                                            // Create new token (id will be auto-generated)
                                            SpotifyTokenDomain newToken = new SpotifyTokenDomain(
                                                    null,
                                                    accessToken,
                                                    refreshToken,
                                                    expiresIn,
                                                    fetchedAt,
                                                    spotifyUserId,
                                                    spotifyEmail,
                                                    spotifyDisplayName
                                            );
                                            return spotifyTokenPort.save(newToken);
                                        }));
                            });
                });
    }

    public String buildAuthorizeUrl(String state) {

        return authBaseUrl
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode(scopes, StandardCharsets.UTF_8)
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
    }


    public Mono<JsonNode> getCurrentUserProfile(String accessToken) {
        System.out.println("Calling /v1/me with token: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");
        return webClientAuth.get()
                .uri("https://api.spotify.com/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    System.err.println("Spotify /v1/me error: " + clientResponse.statusCode() + " - " + body);
                                    return Mono.error(new IllegalStateException(
                                            "Spotify API error " + clientResponse.statusCode() + ": " + body
                                    ));
                                })
                )
                .bodyToMono(JsonNode.class);
    }

}