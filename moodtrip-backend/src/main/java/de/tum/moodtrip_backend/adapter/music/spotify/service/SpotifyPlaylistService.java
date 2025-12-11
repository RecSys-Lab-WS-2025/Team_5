package de.tum.moodtrip_backend.adapter.music.spotify.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SpotifyPlaylistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpotifyPlaylistService.class);

    private final WebClient webClient;
    private final AuthService authService;

    public SpotifyPlaylistService(WebClient.Builder webClientBuilder, AuthService authService) {
        this.webClient = webClientBuilder.baseUrl("https://api.spotify.com").build();
        this.authService = authService;
    }


    public Mono<String> createPlaylist(String name, boolean isPublic, String description, Long userId) {
        LOGGER.info("Creating Spotify playlist '{}' for user {}", name, userId);
        return authService.getAccessToken(userId)
                .flatMap(token -> webClient.post()
                        .uri("/v1/me/playlists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .bodyValue(Map.of(
                                "name", name,
                                "public", isPublic,
                                "description", description
                        ))
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .map(json -> json.path("id").asText(null))
                        .doOnNext(id -> LOGGER.info("Spotify playlist created with ID: {}", id))
                );
    }

    public Mono<Void> addTracksToPlaylist(String playlistId, List<String> trackIds, Long userId) {
        LOGGER.info("Adding {} tracks to playlist {} for user {}", trackIds.size(), playlistId, userId);
        List<String> uris = trackIds.stream()
                .map(id -> "spotify:track:" + id)
                .toList();

        return authService.getAccessToken(userId)
                .flatMap(token -> webClient.post()
                        .uri("/v1/playlists/{playlist_id}/tracks", playlistId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .bodyValue(Map.of("uris", uris))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .doOnSuccess(v -> LOGGER.info("Tracks successfully added to playlist {}", playlistId))
                );
    }
}
