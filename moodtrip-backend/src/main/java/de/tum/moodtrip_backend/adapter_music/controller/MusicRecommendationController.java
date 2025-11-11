package de.tum.moodtrip_backend.adapter_music.controller;

import de.tum.moodtrip_backend.adapter_chatbot.controller.ChatController;
import de.tum.moodtrip_backend.adapter_music.mapper.EmotionToFeatureMapper;
import de.tum.moodtrip_backend.adapter_music.service.MusicRecommendationService;
import de.tum.moodtrip_backend.adapter_music.pojo.FeaturePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/music")
public class MusicRecommendationController {

    private final MusicRecommendationService recommendationService;
    private final EmotionToFeatureMapper mapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(MusicRecommendationController.class);

    public MusicRecommendationController(MusicRecommendationService recommendationService,
                                         EmotionToFeatureMapper mapper) {
        this.recommendationService = recommendationService;
        this.mapper = mapper;
    }

    @GetMapping("/recommend")
    public Mono<String> recommend(@RequestParam String emotion) {
        FeaturePair features = mapper.map(emotion);

        return recommendationService.recommendByEmotion(emotion, features, 20)
                .flatMap(json ->
                        recommendationService.createSpotifyPlaylistFromRecommendation(json, emotion)
                )
                .map(playlistUrl ->
                        "✅ Spotify playlist link:https://open.spotify.com/playlist/" + playlistUrl
                )
                .onErrorResume(e -> {
                    LOGGER.error("Error while extracting emotions", e);
                    return Mono.just("❌ error:" + e.getMessage());
                });
    }
}