package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EmotionCategoryScore;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiRating;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffsetUpdateResult;
import de.tum.moodtrip_backend.core.port.PoiRatingPort;
import de.tum.moodtrip_backend.core.port.EmotionCategoryScorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class PoiRatingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoiRatingService.class);
    private final PoiRatingPort poiRatingPort;
    private final EmotionCategoryScorePort scorePort;
    private final UserPreferenceOffsetService offsetService;
    private final TransactionalOperator transactionalOperator;
    private final double globalLearningRate;
    private final double globalRegularization;

    public PoiRatingService(PoiRatingPort poiRatingPort,
                            EmotionCategoryScorePort scorePort,
                            UserPreferenceOffsetService offsetService,
                            TransactionalOperator transactionalOperator,
                            @Value("${app.global-mapping.learning-rate:0.01}") double globalLearningRate,
                            @Value("${app.global-mapping.regularization:0.001}") double globalRegularization) {
        this.poiRatingPort = poiRatingPort;
        this.scorePort = scorePort;
        this.offsetService = offsetService;
        this.transactionalOperator = transactionalOperator;
        this.globalLearningRate = globalLearningRate;
        this.globalRegularization = globalRegularization;
    }

    public Mono<PoiRating> getRating(Long userId, String poiId, Emotion emotion) {
        return poiRatingPort.findByUserPoiAndEmotion(userId, poiId, emotion);
    }

    public Mono<PoiRating> submitRating(Long userId, String poiId, PoiCategory category, Emotion emotion, Double rating) {
        LOGGER.info("Processing rating submission: userId={}, poiId={}, category={}, emotion={}, rating={}", userId, poiId, category, emotion, rating);

        if (rating == null || rating < 0.5 || rating > 5.0) {
            return Mono.error(new IllegalArgumentException("Rating must be between 0.5 and 5"));
        }

        return transactionalOperator.transactional(
                poiRatingPort.findByUserPoiAndEmotion(userId, poiId, emotion)
                .flatMap(existingRating -> {
                    LOGGER.info("Updating existing rating for userId={}, poiId={}, emotion={}. Old rating: {}, New rating: {}", 
                            userId, poiId, emotion, existingRating.rating(), rating);
                    
                    PoiRating updatedRating = new PoiRating(
                            existingRating.id(), 
                            userId, 
                            poiId, 
                            category, 
                            emotion, 
                            rating, 
                            LocalDateTime.now()
                    );
                    
                    return initGlobalScore(emotion, category)
                            .flatMap(mapping -> poiRatingPort.save(updatedRating)
                                    .flatMap(saved -> offsetService.updateUserPreferenceOffsetWithGlobal(userId, emotion, category, rating, mapping.score(), false)
                                            .flatMap(offsetResult -> updateGlobalScore(mapping, offsetResult, false)
                                                    .thenReturn(saved))));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    LOGGER.info("Creating new rating for userId={}, poiId={}, emotion={}. Rating: {}", 
                            userId, poiId, emotion, rating);
                    
                    PoiRating newRating = new PoiRating(null, userId, poiId, category, emotion, rating, LocalDateTime.now());
                    
                    return initGlobalScore(emotion, category)
                            .flatMap(mapping -> poiRatingPort.save(newRating)
                                    .flatMap(saved -> offsetService.updateUserPreferenceOffsetWithGlobal(userId, emotion, category, rating, mapping.score(), true)
                                            .flatMap(offsetResult -> updateGlobalScore(mapping, offsetResult, true)
                                                    .thenReturn(saved))));
                }))
        );
    }

    private Mono<EmotionCategoryScore> initGlobalScore(Emotion emotion, PoiCategory category) {
        return scorePort.findByEmotionAndCategory(emotion, category)
                .defaultIfEmpty(new EmotionCategoryScore(null, emotion, category, 3.5, 50L));
    }

    private Mono<Void> updateGlobalScore(EmotionCategoryScore mapping, UserPreferenceOffsetUpdateResult offsetResult, boolean isUpdate) {
        double wOld = mapping.score();
        double error = offsetResult.error();
        double wNew = wOld + globalLearningRate * (error - globalRegularization * wOld);

        long currentCount = mapping.ratingCount() == null ? 0L : mapping.ratingCount();
        long newCount = currentCount + (isUpdate ? 0 : 1);

        EmotionCategoryScore updated = mapping.withScore(wNew).withCount(newCount);
        return scorePort.save(updated).then();
    }
}
