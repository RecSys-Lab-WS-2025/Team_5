package de.tum.moodtrip_backend.adapter_music.service;

import com.fasterxml.jackson.databind.JsonNode;
import de.tum.moodtrip_backend.adapter_music.mapper.PlaylistMapper;
import de.tum.moodtrip_backend.adapter_music.pojo.FeaturePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import java.util.stream.StreamSupport;

@Service
public class MusicRecommendationService {

    private final WebClient webClient;
    private final AuthService authService;
    @Autowired
    private SpotifyPlaylistService spotifyPlaylistService;
    @Autowired
    private PlaylistMapper playlistMapper;


    private final String spotifyApiUrl = "api.spotify.com";
    private final String reccoBeatsUrl = "api.reccobeats.com";

    public MusicRecommendationService(WebClient.Builder webClientBuilder, AuthService authService) {
        this.webClient = webClientBuilder.build();
        this.authService = authService;
    }

    private Mono<String> getNeutralPlaylistId() {
        return authService.getAccessToken()
                .flatMap(token -> webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host(spotifyApiUrl)
                                .path("/v1/search")
                                .queryParam("q", "neutral")
                                .queryParam("type", "playlist")
                                .queryParam("limit", 1)
                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .map(json -> json.path("playlists").path("items").get(0).path("id").asText()));
    }


    public Mono<String> findPlaylistIdByEmotion(String emotionKeyword) {
        System.out.println("Searching Spotify for playlist matching emotion: " + emotionKeyword);
        Mono<String> fallbackId = getNeutralPlaylistId();



        return authService.getAccessToken()
                .flatMap(token ->
                        webClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .scheme("https")
                                        .host(spotifyApiUrl)
                                        .path("/v1/search")
                                        .queryParam("q", emotionKeyword)
                                        .queryParam("type", "playlist")
                                        .queryParam("limit", 1)
                                        .build())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(JsonNode.class)
                                .map(json -> json
                                        .path("playlists")
                                        .path("items")
                                        .path(0)
                                        .path("id")
                                        .asText()
                                )
                                .map(str -> {
                                    System.out.println("Found playlist ID for emotion [" + emotionKeyword + "]: " + str);
                                    return str;
                                }).filter(id -> id != null && !id.isEmpty())
                                .switchIfEmpty(fallbackId)
                );
    }

    public Mono<List<String>> getFirstFiveTrackIdsOfPlaylist(String playlistId) {
        System.out.printf("Fetching first five track IDs from Spotify playlist: %s%n", playlistId);
        return authService.getAccessToken()
                .flatMap(token ->
                        webClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .scheme("https")
                                        .host(spotifyApiUrl)
                                        .path("/v1/playlists/" + playlistId + "/tracks")
                                        .queryParam("limit", 5)
                                        .build())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(JsonNode.class)
                                .map(json -> StreamSupport.stream(
                                                json.path("items").spliterator(), false)
                                        .map(item -> item.path("track").path("id").asText())
                                        .filter(id -> id != null && !id.isEmpty())
                                        .toList()
                                )
                );
    }

    public Mono<JsonNode> recommendByEmotion(String emotionKeyword, FeaturePair featurePair, int limit) {
        float energy = featurePair.energy();
        float valence = featurePair.valence();
        System.out.printf(" Generating music recommendation for emotion: %s (energy: %.2f, valence: %.2f)%n",
                emotionKeyword, energy, valence);
        return findPlaylistIdByEmotion(emotionKeyword)
                .flatMap(this::getFirstFiveTrackIdsOfPlaylist)
                .flatMap(trackIds -> {
                    String seedsParam = String.join(",", trackIds);
                    System.out.println("Seed track IDs for emotion [" + emotionKeyword + "] → " + seedsParam);

                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .scheme("https")
                                    .host(reccoBeatsUrl)
                                    .path("/v1/track/recommendation")
                                    .queryParam("size", limit)
                                    .queryParam("energy", energy)
                                    .queryParam("valence", valence)
                                    .queryParam("seeds", seedsParam)
                                    .build())
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .onErrorResume(e -> {
                                System.err.println("ReccoBeats call failed: " + e.getMessage());
                                return Mono.empty();
                            });
                });
    }

    public Mono<String> createSpotifyPlaylistFromRecommendation(JsonNode recommendationJson, String mood) {
        List<String> trackIds = playlistMapper.extractTrackIdsFromJson(recommendationJson);

        String playlistName = "MoodTrip - " + mood + " Vibes";
        String description = "A playlist generated based on your mood: " + mood;
        Mono<String> playlistId = spotifyPlaylistService.createPlaylist(playlistName, true, description);
        return playlistId.flatMap(id ->
                spotifyPlaylistService.addTracksToPlaylist(id, trackIds)
                        .thenReturn(id)
        );
    }
}