package de.tum.moodtrip_backend.api.controller;

import de.tum.moodtrip_backend.api.dto.PoiRatingRequest;
import de.tum.moodtrip_backend.api.security.JwtService;
import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiRating;
import de.tum.moodtrip_backend.core.service.PoiRatingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/ratings")
public class PoiRatingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoiRatingController.class);
    private final PoiRatingService ratingService;
    private final JwtService jwtService;

    public PoiRatingController(PoiRatingService ratingService, JwtService jwtService) {
        this.ratingService = ratingService;
        this.jwtService = jwtService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PoiRating> submitRating(@Valid @RequestBody PoiRatingRequest request, Authentication authentication) {
        Long userId = jwtService.extractUserId(authentication);
        LOGGER.info("Received rating submission: userId={}, poiId={}, category={}, emotion={}, rating={}",
                userId, request.poiId(), request.category(), request.emotion(), request.rating());
        
        return ratingService.submitRating(
                userId,
                request.poiId(),
                PoiCategory.fromString(request.category()),
                Emotion.fromString(request.emotion()),
                request.rating()
        ).doOnSuccess(r -> LOGGER.info("Rating submission successful for userId={} and poiId={}", userId, request.poiId()));
    }

    @GetMapping
    public Mono<PoiRating> getRating(
            @RequestParam String poiId,
            @RequestParam String emotion,
            Authentication authentication
    ) {
        Long userId = jwtService.extractUserId(authentication);
        return ratingService.getRating(userId, poiId, Emotion.fromString(emotion));
    }
}
