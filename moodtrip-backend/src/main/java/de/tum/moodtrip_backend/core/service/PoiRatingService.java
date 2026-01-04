package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EmotionCategoryScore;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiRating;
import de.tum.moodtrip_backend.core.port.EmotionCategoryScorePort;
import de.tum.moodtrip_backend.core.port.PoiRatingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class PoiRatingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoiRatingService.class);
    private final PoiRatingPort poiRatingPort;
    private final EmotionCategoryScorePort scorePort;
    private final UserPreferenceOffsetService offsetService;

    public PoiRatingService(PoiRatingPort poiRatingPort,
                            EmotionCategoryScorePort scorePort,
                            UserPreferenceOffsetService offsetService) {
        this.poiRatingPort = poiRatingPort;
        this.scorePort = scorePort;
        this.offsetService = offsetService;
    }

    public Mono<PoiRating> getRating(Long userId, String poiId, Emotion emotion) {
        return poiRatingPort.findByUserPoiAndEmotion(userId, poiId, emotion);
    }

    public Mono<PoiRating> submitRating(Long userId, String poiId, PoiCategory category, Emotion emotion, Double rating) {
        LOGGER.info("Processing rating submission: userId={}, poiId={}, category={}, emotion={}, rating={}", userId, poiId, category, emotion, rating);

        if (rating == null || rating < 0.5 || rating > 5.0) {
            return Mono.error(new IllegalArgumentException("Rating must be between 0.5 and 5"));
        }

        return poiRatingPort.findByUserPoiAndEmotion(userId, poiId, emotion)
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
                    
                    return poiRatingPort.save(updatedRating)
                            .flatMap(saved -> updateCategoryScore(emotion, category, rating, existingRating.rating())
                                    .then(offsetService.updateUserPreferenceOffset(userId, emotion, category, rating, false))
                                    .thenReturn(saved));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    LOGGER.info("Creating new rating for userId={}, poiId={}, emotion={}. Rating: {}", 
                            userId, poiId, emotion, rating);
                    
                    PoiRating newRating = new PoiRating(null, userId, poiId, category, emotion, rating, LocalDateTime.now());
                    
                    return poiRatingPort.save(newRating)
                            .flatMap(saved -> updateCategoryScore(emotion, category, rating, null)
                                    .then(offsetService.updateUserPreferenceOffset(userId, emotion, category, rating, true))
                                    .thenReturn(saved));
                }));
    }

    //update the average score for the given category and emotion
    private Mono<Void> updateCategoryScore(Emotion emotion, PoiCategory category, Double newRating, Double oldRating) {
        boolean isUpdate = oldRating != null;
        String actionType = isUpdate ? "Updating" : "Adding";
        
        LOGGER.info("{} score for category [{}] under emotion [{}]", actionType, category, emotion);

        return scorePort.findByEmotionAndCategory(emotion, category)
                .switchIfEmpty(Mono.defer(() -> {
                    LOGGER.info("No existing score found for emotion={} and category={}. Initializing with default values.", emotion, category);
                    return Mono.just(new EmotionCategoryScore(null, emotion, category, 3.5, 50L));
                }))
                .flatMap(scoreObj -> {
                    long oldCount = scoreObj.ratingCount();
                    double oldAvg = scoreObj.score();
                    double newAvg;
                    long newCount;

                    if (isUpdate) {
                        newAvg = oldAvg + (newRating - oldRating) / oldCount;
                        newCount = oldCount;
                    } else {
                        newAvg = (oldAvg * oldCount + newRating) / (oldCount + 1);
                        newCount = oldCount + 1;
                    }

                    LOGGER.debug("Calculating new score: emotion={}, category={}, oldAvg={}, newAvg={}, oldCount={}, newCount={}", 
                            emotion, category, oldAvg, newAvg, oldCount, newCount);
                    
                    return scorePort.save(scoreObj.withScore(newAvg).withCount(newCount));
                })
                .doOnSuccess(s -> LOGGER.info("The new score for category [{}] under emotion [{}] is [{}] (based on [{}] ratings)", 
                        category, emotion, s.score(), s.ratingCount()))
                .then();
    }
}
