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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/music")
@Tag(name = "Music Recommendations", description = "APIs for generating emotion-based music recommendations and Spotify playlists")
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

    @Operation(
        summary = "Get music recommendations based on emotion",
        description = "Generates Spotify playlist recommendations based on detected user emotion and creates a shareable playlist",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Playlist created successfully, returns Spotify playlist URL"
        )
    })
    @GetMapping("/recommend")
    public Mono<String> recommend(
            @Parameter(description = "User's current emotion", required = true) @RequestParam String emotion, 
            @Parameter(description = "Conversation ID", required = true) @RequestParam Long convId,
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

    @Operation(
        summary = "Get music recommendation history",
        description = "Retrieves all previously generated music playlists for a specific conversation",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Music recommendation history retrieved successfully"
        )
    })
    @GetMapping("/{conversationId}")
    public Flux<MusicRecommendationDomain> getHistory(
            @Parameter(description = "Conversation ID", required = true) @PathVariable Long conversationId,
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