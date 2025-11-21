package de.tum.moodtrip_backend.adapter_music.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import de.tum.moodtrip_backend.adapter_music.mapper.PlaylistMapper;
import de.tum.moodtrip_backend.adapter_music.pojo.FeaturePair;
import de.tum.moodtrip_backend.core.model.MusicRecommendationDomain;
import de.tum.moodtrip_backend.core.port.MusicRecommendationPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MusicRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(MusicRecommendationService.class);

    private final WebClient webClient;
    private final AuthService authService;

    private final SpotifyPlaylistService spotifyPlaylistService;
    private final PlaylistMapper playlistMapper;
    private final MusicRecommendationPort musicRecommendationPort;


    private final String spotifyApiUrl = "api.spotify.com";
    private final String reccoBeatsUrl = "api.reccobeats.com";
    private String default_seeds = "1q4BCQssFe74UJmnWt5lov,2KslE17cAJNHTsI2MI0jb2,3rUGC1vUpkDG9CZFHMur1t,2HRgqmZQC0MC7GeNuDIXHN,0WtM2NBVQNNJLh6scP13H8";

    public MusicRecommendationService(WebClient.Builder webClientBuilder, AuthService authService, SpotifyPlaylistService spotifyPlaylistService, PlaylistMapper playlistMapper, MusicRecommendationPort musicRecommendationPort) {
        this.webClient = webClientBuilder.build();
        this.authService = authService;
        this.spotifyPlaylistService = spotifyPlaylistService;
        this.playlistMapper = playlistMapper;
        this.musicRecommendationPort = musicRecommendationPort;
    }

    private Mono<String> getNeutralPlaylistId(Long userId) {
        return authService.getAccessToken(userId)
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
                        .map(json -> json.path("playlists").path("items").path(0).path("id").asText()));
    }


    public Mono<String> findPlaylistIdByEmotion(String emotionKeyword, Long userId) {
        logger.info("Searching Spotify for playlist matching emotion: {}", emotionKeyword);
        Mono<String> fallbackId = getNeutralPlaylistId(userId);


        return authService.getAccessToken(userId)
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
                                    logger.info("Found playlist ID for emotion [{}]: {}", emotionKeyword, str);
                                    return str;
                                }).filter(id -> id != null && !id.isEmpty())
                                .switchIfEmpty(fallbackId)
                );
    }

    public Mono<List<String>> getFirstFiveTrackIdsOfPlaylist(String playlistId, Long userId) {
        logger.info("Fetching first five track IDs from Spotify playlist: {}", playlistId);
        return authService.getAccessToken(userId)
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
                                        .filter(id -> !id.isEmpty())
                                        .toList()
                                )
                );
    }

    public Mono<JsonNode> recommendByEmotion(String emotionKeyword, FeaturePair featurePair, int limit, Long userId) {
        float energy = featurePair.energy();
        float valence = featurePair.valence();
        logger.info("Generating music recommendation for emotion: {} (energy: {}, valence: {})",
                emotionKeyword, energy, valence);
        return findPlaylistIdByEmotion(emotionKeyword, userId)
                .flatMap(playlistid -> getFirstFiveTrackIdsOfPlaylist(playlistid, userId))
                .flatMap(trackIds -> {
                    String seedsParam = String.join(",", trackIds);
                    String seeds = seedsParam.isEmpty() ? default_seeds : seedsParam;

                    logger.debug("Seed track IDs for emotion [{}] → {}", emotionKeyword, seedsParam);

                    return webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .scheme("https")
                                    .host(reccoBeatsUrl)
                                    .path("/v1/track/recommendation")
                                    .queryParam("size", limit)
                                    .queryParam("energy", energy)
                                    .queryParam("valence", valence)
                                    .queryParam("seeds", seeds)
                                    .build())
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .onErrorResume(e -> {
                                logger.error("ReccoBeats call failed: {}", e.getMessage());
                                return Mono.empty();
                            });
                });
    }

    public Mono<String> createSpotifyPlaylistFromRecommendation(JsonNode recommendationJson, String mood, Long userId, Long convId) {
        String playlistName = "MoodTrip - " + mood + " Vibes";
        String description = "A playlist generated based on your mood: " + mood;
        return Mono.fromCallable(() -> playlistMapper.extractTrackIdsFromJson(recommendationJson))
                .flatMap(trackIds ->
                        spotifyPlaylistService.createPlaylist(playlistName, true, description, userId)
                                .flatMap(playlistId ->
                                        spotifyPlaylistService.addTracksToPlaylist(playlistId, trackIds, userId)
                                                .then(Mono.defer(() -> {
                                                    String playlistUrl = "https://open.spotify.com/playlist/" + playlistId;

                                                    MusicRecommendationDomain domain = new MusicRecommendationDomain(
                                                            null,
                                                            convId,
                                                            mood + " playlist",
                                                            playlistUrl,
                                                            LocalDateTime.now());

                                                    logger.info("Saving playlist to DB: {}", playlistUrl);

                                                    return musicRecommendationPort.save(domain)
                                                            .thenReturn(playlistId);
                                                }))
                                )
                );
    }

    public Flux<MusicRecommendationDomain> getPlaylistsByConversation(Long convId) {
        return musicRecommendationPort.findByConversationId(convId);
    }
}