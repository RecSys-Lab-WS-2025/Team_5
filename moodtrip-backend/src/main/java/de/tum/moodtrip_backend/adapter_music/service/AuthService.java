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
     * Get access token for a specific user, with automatic refresh if expired
     */
    public Mono<String> getAccessToken(Long userId) {
        return spotifyTokenPort.findByUserId(userId).next()
                .flatMap(spotifyTokenEntity -> {
                    long currentTime = System.currentTimeMillis() / 1000;
                    long tokenAge = currentTime - spotifyTokenEntity.fetchedAt();

                    // Token expires in spotifyToken.getExpiresIn() seconds, refresh if less than 5 minutes left
                    if (tokenAge + 300 >= spotifyTokenEntity.expiresIn()) {
                        System.out.println("Token expired or about to expire, refreshing...");
                        return refreshAndSaveToken(userId, spotifyTokenEntity.refreshToken());
                    }

                    return Mono.just(spotifyTokenEntity.accessToken());
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
     * Refresh token and save to database
     */
    private Mono<String> refreshAndSaveToken(Long userId, String refreshToken) {
        return webClientAuth.post()
                .uri("/api/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", refreshToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(json -> {
                    String newAccessToken = json.path("access_token").asText();
                    long expiresIn = json.path("expires_in").asLong();
                    long fetchedAt = System.currentTimeMillis() / 1000;

                    String newRefreshToken = json.path("refresh_token").asText("");
                    if (newRefreshToken.isEmpty()) {
                        newRefreshToken = refreshToken;
                    }

                    return saveOrUpdateToken(userId, newAccessToken, newRefreshToken, expiresIn, fetchedAt)
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


    public Mono<SpotifyTokenDomain> exchangeCodeForToken(String code, Long userId) {
        System.out.printf("Exchanging code for token, code: %s, userId: %d%n", code, userId);
        return webClientAuth.post()
                .uri("/api/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("code", code)
                        .with("redirect_uri", redirectUri)
                )
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(json -> {
                    String accessToken = json.path("access_token").asText();
                    String refreshToken = json.path("refresh_token").asText();
                    long expiresIn = json.path("expires_in").asLong();
                    long fetchedAt = System.currentTimeMillis() / 1000;
                    return saveOrUpdateToken(userId, accessToken, refreshToken, expiresIn, fetchedAt);
                });
    }

    private Mono<SpotifyTokenDomain> saveOrUpdateToken(Long userId, String accessToken, String refreshToken, long expiresIn, long fetchedAt) {
        return spotifyTokenPort.findByUserId(userId)
                .next()
                .map(existingToken -> new SpotifyTokenDomain(
                        existingToken.id(),
                        userId,
                        accessToken,
                        refreshToken,
                        expiresIn,
                        fetchedAt
                ))
                .defaultIfEmpty(
                        new SpotifyTokenDomain(
                                null,
                                userId,
                                accessToken,
                                refreshToken,
                                expiresIn,
                                fetchedAt
                        )
                )
                .flatMap(spotifyTokenPort::save);
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
        return webClientAuth.get()
                .uri("https://api.spotify.com/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

}