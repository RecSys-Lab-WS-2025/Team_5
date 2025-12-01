package de.tum.moodtrip_backend.adapter_music.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.adapter_music.mapper.EmotionToFeatureMapper;
import de.tum.moodtrip_backend.adapter_music.pojo.FeaturePair;
import de.tum.moodtrip_backend.adapter_music.service.MusicRecommendationService;
import de.tum.moodtrip_backend.core.model.MusicRecommendationDomain;
import de.tum.moodtrip_backend.core.port.MusicRecommendationPort;
import de.tum.moodtrip_backend.core.service.ConversationDomainService;
import de.tum.moodtrip_backend.security.JwtService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/music")
public class MusicRecommendationController {

    private final MusicRecommendationService recommendationService;
    private final EmotionToFeatureMapper mapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(MusicRecommendationController.class);
    private final MusicRecommendationPort musicRecommendationPort;
    private final JwtService jwtService;
    private final ConversationDomainService conversationService;

    public MusicRecommendationController(MusicRecommendationService recommendationService,
                                         EmotionToFeatureMapper mapper, 
                                         MusicRecommendationPort musicRecommendationPort,
                                         JwtService jwtService,
                                         ConversationDomainService conversationService) {
        this.recommendationService = recommendationService;
        this.mapper = mapper;
        this.musicRecommendationPort = musicRecommendationPort;
        this.jwtService = jwtService;
        this.conversationService = conversationService;
    }

    @GetMapping("/recommend")
    public Mono<String> recommend(
            @RequestParam String emotion, 
            @RequestParam Long convId,
            Authentication authentication) {
        
        Long userId = jwtService.extractUserId(authentication);
        FeaturePair features = mapper.map(emotion);

        return recommendationService.recommendByEmotion(emotion, features, 20, userId)
                .flatMap(json ->
                        recommendationService.createSpotifyPlaylistFromRecommendation(json, emotion, userId, convId)
                )
                .map(playlistUrl -> "✅ Spotify playlist link:https://open.spotify.com/playlist/" + playlistUrl)
                .onErrorResume(e -> {
                    LOGGER.error("Error while creating music recommendation", e);
                    return Mono.just("❌ error:" + e.getMessage());
                });
    }

    @GetMapping("/{conversationId}")
    public Flux<MusicRecommendationDomain> getHistory(
            @PathVariable Long conversationId,
            Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);

        return conversationService.getConversationById(conversationId)
                .flatMapMany(conversation -> {
                    if (!conversation.userId().equals(userId)) {
                        return Flux.error(new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access denied: This conversation does not belong to you"
                        ));
                    }
                    return recommendationService.getPlaylistsByConversation(conversationId);
                });
    }
}