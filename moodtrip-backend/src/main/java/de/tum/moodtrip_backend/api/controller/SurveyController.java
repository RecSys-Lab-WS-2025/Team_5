package de.tum.moodtrip_backend.api.controller;


import static java.time.temporal.ChronoUnit.DAYS;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geojson.FeatureCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.adapter.music.spotify.mapper.EmotionToFeatureMapper;
import de.tum.moodtrip_backend.adapter.music.spotify.pojo.FeaturePair;
import de.tum.moodtrip_backend.adapter.music.spotify.service.MusicRecommendationService;
import de.tum.moodtrip_backend.api.dto.SurveyRequest;
import de.tum.moodtrip_backend.api.dto.SurveyResponse;
import de.tum.moodtrip_backend.api.mapper.GeoJsonRouteMapper;
import de.tum.moodtrip_backend.api.mapper.SurveyDtoMapper;
import de.tum.moodtrip_backend.api.security.JwtService;
import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.RouteGenerationResult;
import de.tum.moodtrip_backend.core.model.RouteStatus;
import de.tum.moodtrip_backend.core.port.SurveyPort;
import de.tum.moodtrip_backend.core.service.ConversationDomainService;
import de.tum.moodtrip_backend.core.service.RouteService;
import de.tum.moodtrip_backend.core.service.UserDomainService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/surveys")
@Validated
public class SurveyController {

    private final SurveyPort surveyPort;
    private final SurveyDtoMapper surveyDtoMapper;
    private final GeoJsonRouteMapper geoJsonRouteMapper;
    private final JwtService jwtService;
    private final ConversationDomainService conversationDomainService;
    private final RouteService routeService;
    private final UserDomainService userDomainService;
    private final MusicRecommendationService musicRecommendationService;
    private final EmotionToFeatureMapper emotionToFeatureMapper;
    private static final Logger logger = LoggerFactory.getLogger(SurveyController.class);
    @Value("${app.energy.factor.default:1.0}")
    private double defaultEnergyFactor;
    @Value("${app.energy.factor.happy:1.1}")
    private double happyEnergyFactor;

    @Value("${app.energy.factor.sad:0.9}")
    private double sadEnergyFactor;

    @Value("${app.energy.factor.stressed:1.2}")
    private double stressedEnergyFactor;

    @Value("${app.energy.factor.high:1.3}")
    private double highEnergyFactor;

    @Value("${app.energy.factor.low:0.6}")
    private double lowEnergyFactor;


    public SurveyController(final SurveyPort surveyPort, final SurveyDtoMapper surveyDtoMapper, final GeoJsonRouteMapper geoJsonRouteMapper, final JwtService jwtService, final ConversationDomainService conversationDomainService, final RouteService routeService, final UserDomainService userDomainService, final MusicRecommendationService musicRecommendationService, final EmotionToFeatureMapper emotionToFeatureMapper) {
        this.surveyPort = surveyPort;
        this.surveyDtoMapper = surveyDtoMapper;
        this.geoJsonRouteMapper = geoJsonRouteMapper;
        this.jwtService = jwtService;
        this.conversationDomainService = conversationDomainService;
        this.routeService = routeService;
        this.userDomainService = userDomainService;
        this.musicRecommendationService = musicRecommendationService;
        this.emotionToFeatureMapper = emotionToFeatureMapper;
    }


    @PostMapping
    public Mono<SurveyResponse> submitSurvey(
            @Valid @RequestBody SurveyRequest request,
            @RequestParam @NotNull(message = "Conversation ID cannot be null") Long conversationId,
            Authentication authentication) {

        Long userId = jwtService.extractUserId(authentication);
        logger.info("Received survey submission for conversationId: {} from userId: {}", conversationId, userId);

        return conversationDomainService.getConversationById(conversationId)
                .flatMap(conversation -> {
                    if (!conversation.userId().equals(userId)) {
                        logger.warn("Access denied for survey submission. User {} tried to submit for conversation {}", userId, conversationId);
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Access denied: This conversation does not belong to you"
                        ));
                    }
                    return Mono.just(request)
                            .map(req -> surveyDtoMapper.requestToDomain(req, conversationId, userId))
                            .flatMap(surveyPort::save)
                            .doOnNext(s -> logger.info("Survey saved successfully for conversationId: {}", conversationId))
                            .flatMap(surveyDomain -> {
                                logger.info("Survey categories for conversationId {}: {}", conversationId, surveyDomain.poiCategories());
                                Emotion fallbackEmotion = conversation.emotion() != null ? conversation.emotion() : Emotion.NEUTRAL;
                                Mono<Map<Emotion, Double>> emotionWeightsMono = conversationDomainService.getLatestEmotionWeights(conversationId)
                                        .map(weights -> weights.isEmpty() ? Map.of(fallbackEmotion, 1.0) : weights)
                                        .defaultIfEmpty(Map.of(fallbackEmotion, 1.0));

                                Mono<List<RouteGenerationResult>> routeResultsMono = emotionWeightsMono.flatMap(emotionWeights -> {
                                                long days = DAYS.between(surveyDomain.startDate(), surveyDomain.endDate()) + 1;

                                                double energyScore = emotionWeights.entrySet().stream()
                                                        .mapToDouble(entry -> {
                                                            double factor = switch (entry.getKey()) {
                                                                case ENERGIZED -> highEnergyFactor;
                                                                case JOYFUL -> happyEnergyFactor;
                                                                case NEUTRAL, CALM -> defaultEnergyFactor;
                                                                case STRESSED -> stressedEnergyFactor;
                                                                case SAD -> sadEnergyFactor;
                                                                case TIRED -> lowEnergyFactor;
                                                            };
                                                            return entry.getValue() * factor;
                                                        })
                                                        .sum();


                                                int dynamicLimit = (int) Math.round((2 + (days - 1) * 3) * energyScore);
                                                logger.info("Trip duration: {} days, Mood energy score: {}. Set dynamic POI limit to: {}",
                                                        days, String.format("%.2f", energyScore), dynamicLimit);

                                                // Determine if we're in mock mode by checking if the EmotionResult content contains mock indicator
                                                boolean isMocked = conversation.emotionResult().content().contains("THIS IS A MOCK RESPONSE");
                                                logger.info("Mock mode for route generation: {} (detected from emotion result content)", isMocked);

                                                return routeService.getMultipleRoutes(conversationId,
                                                        userId,
                                                        surveyDomain.latitude(), surveyDomain.longitude(), surveyDomain.poiCategories(),
                                                        surveyDomain.rangeMeters(),
                                                        emotionWeights,
                                                        dynamicLimit,
                                                        surveyDomain.locationName(),
                                                        (int) days,
                                                        isMocked);
                                            })
                                        .cache();

                                Mono<String> spotifyMono = routeResultsMono
                                        .filter(results -> results.stream().anyMatch(r -> r.status() == RouteStatus.SUCCEEDED))
                                        .flatMap(results -> userDomainService.findById(userId)
                                                .flatMap(user -> {
                                                    if (user.hasSpotifyAuthorization()) {
                                                        logger.info("Generating spotify recommendation for user: {}", userId);
                                                        return conversationDomainService.getConversationById(conversationId)
                                                                .flatMap(conv -> {
                                                                    String emotion = conv.emotion().toString();
                                                                    FeaturePair features = emotionToFeatureMapper.map(emotion);
                                                                    return musicRecommendationService.recommendByEmotion(emotion, features, 20, userId)
                                                                            .flatMap(json -> musicRecommendationService.createSpotifyPlaylistFromRecommendation(json, emotion, userId, conversationId))
                                                                            .map(playlistId -> "https://open.spotify.com/playlist/" + playlistId);
                                                                });
                                                    }
                                                    return Mono.empty();
                                                }))
                                        .onErrorResume(e -> {
                                            logger.error("Failed to generate music recommendation", e);
                                            return Mono.empty();
                                        });

                                return routeResultsMono.flatMap(results -> {
                                    // Filter successful results
                                    List<RouteGenerationResult> successfulResults = results.stream()
                                            .filter(r -> r.status() == RouteStatus.SUCCEEDED && r.route() != null)
                                            .collect(Collectors.toList());

                                    if (!successfulResults.isEmpty()) {
                                        long days = DAYS.between(surveyDomain.startDate(), surveyDomain.endDate()) + 1;
                                        String emotionStr = conversation.emotion().name();

                                        // Map each successful route to a FeatureCollection with route type
                                        List<FeatureCollection> routeCollections = successfulResults.stream()
                                                .map(result -> geoJsonRouteMapper.toFeatureCollection(
                                                        result.route(), emotionStr, (int) days, result.routeType()))
                                                .collect(Collectors.toList());

                                        return spotifyMono.defaultIfEmpty("")
                                                .map(link -> SurveyResponse.successMultiple(routeCollections, link.isEmpty() ? null : link));
                                    }

                                    // If no routes succeeded, return failure with the first error message
                                    String userMessage = results.stream()
                                            .filter(r -> r.userMessage() != null)
                                            .map(RouteGenerationResult::userMessage)
                                            .findFirst()
                                            .orElse("I couldn't generate a route due to a routing service error. Please try again.");
                                    return Mono.just(SurveyResponse.failure(userMessage));
                                }).onErrorResume(e -> {
                                    logger.error("Route generation failed for conversationId: {}", conversationId, e);
                                    return Mono.just(SurveyResponse.failure("I couldn't generate a route due to a routing service error. Please try again."));
                                });
                            })
                            .doOnSuccess(r -> logger.info("Survey processing completed. Sending response for conversationId: {}", conversationId));
                });
    }
}
