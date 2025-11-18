package de.tum.moodtrip_backend.adapter_music.service;


import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;


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
    private final WebClient webClientApi;

    private Mono<String> cachedAccessTokenMono;



    public AuthService(WebClient.Builder webClientBuilder) {
        this.webClientAuth = webClientBuilder.baseUrl("https://accounts.spotify.com").build();
        this.webClientApi = webClientBuilder.baseUrl("https://api.spotify.com").build();
    }

    public Mono<String> getAccessToken() {
        if (cachedAccessTokenMono == null) {
            cachedAccessTokenMono = fetchAccessToken()
                    .cache(Duration.ofSeconds(3500));
        }
        return cachedAccessTokenMono;
    }

    private Mono<String> fetchAccessToken() {
        if (token != null && !token.isBlank()) {
            return Mono.just(token);
        }
        return Mono.error(new IllegalStateException("Access token is not configured. Please authorize via OAuth."));
    }




    public Mono<JsonNode> refreshAccessToken(String refreshToken) {
        return webClientAuth.post()
                .uri("https://accounts.spotify.com/api/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("refresh_token", refreshToken))
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public Mono<JsonNode> getCurrentUserProfile(String accessToken) {
        return webClientAuth.get()
                .uri("https://api.spotify.com/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class);
    }


    public Mono<JsonNode> exchangeCodeForToken(String code) {
        System.out.printf("Exchanging code for token, code: %s%n", code);
        return webClientAuth.post()
                .uri("/api/token")
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("code", code)
                        .with("redirect_uri", redirectUri)
                )

                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    public String buildAuthorizeUrl(String state) {
        String scopeParam = scopes.replace(" ", "%20").replace(",", "%20");
        return authBaseUrl
                + "/authorize?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=" + scopeParam
                + "&state=" + state;
    }



}