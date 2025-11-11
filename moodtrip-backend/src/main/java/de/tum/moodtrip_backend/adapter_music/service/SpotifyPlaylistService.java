package de.tum.moodtrip_backend.adapter_music.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpotifyPlaylistService {

    private final WebClient webClient;
    private final AuthService authService;

    public SpotifyPlaylistService(WebClient.Builder webClientBuilder, AuthService authService) {
        this.webClient = webClientBuilder.baseUrl("https://api.spotify.com").build();
        this.authService = authService;
    }




    public Mono<String> createPlaylist( String name, boolean isPublic, String description) {
        return authService.getAccessToken()
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
                        .map(json -> json.get("id").asText())
                );
    }

    public Mono<Void> addTracksToPlaylist(String playlistId, List<String> trackIds) {
        List<String> uris = trackIds.stream()
                .map(id -> "spotify:track:" + id)
                .toList();

        return authService.getAccessToken()
                .flatMap(token -> webClient.post()
                        .uri("/v1/playlists/{playlist_id}/tracks", playlistId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .bodyValue(Map.of("uris", uris))
                        .retrieve()
                        .bodyToMono(Void.class)
                );
    }
}
