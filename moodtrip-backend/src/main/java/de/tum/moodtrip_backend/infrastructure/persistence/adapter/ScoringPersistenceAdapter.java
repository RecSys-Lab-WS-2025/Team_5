package de.tum.moodtrip_backend.infrastructure.persistence.adapter;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EmotionCategoryScore;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiRating;
import de.tum.moodtrip_backend.core.port.EmotionCategoryScorePort;
import de.tum.moodtrip_backend.core.port.PoiRatingPort;
import de.tum.moodtrip_backend.infrastructure.persistence.mapper.ScoringMapper;
import de.tum.moodtrip_backend.infrastructure.persistence.repository.R2dbcEmotionCategoryScoreRepository;
import de.tum.moodtrip_backend.infrastructure.persistence.repository.R2dbcPoiRatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class ScoringPersistenceAdapter implements EmotionCategoryScorePort, PoiRatingPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScoringPersistenceAdapter.class);
    private final R2dbcEmotionCategoryScoreRepository scoreRepository;
    private final R2dbcPoiRatingRepository ratingRepository;

    public ScoringPersistenceAdapter(R2dbcEmotionCategoryScoreRepository scoreRepository,
                                     R2dbcPoiRatingRepository ratingRepository) {
        this.scoreRepository = scoreRepository;
        this.ratingRepository = ratingRepository;
    }

    @Override
    public Mono<EmotionCategoryScore> findByEmotionAndCategory(Emotion emotion, PoiCategory category) {
        LOGGER.debug("Fetching emotion_category_score: emotion={}, category={}", emotion, category);
        return scoreRepository.findByEmotionAndCategory(emotion.name(), category.name())
                .map(ScoringMapper::toDomain);
    }

    @Override
    public Mono<EmotionCategoryScore> findByEmotionAndCategoryForUpdate(Emotion emotion, PoiCategory category) {
        LOGGER.debug("Fetching emotion_category_score for update: emotion={}, category={}", emotion, category);
        return scoreRepository.findByEmotionAndCategoryForUpdate(emotion.name(), category.name())
                .map(ScoringMapper::toDomain);
    }

    @Override
    public Mono<EmotionCategoryScore> save(EmotionCategoryScore score) {
        LOGGER.debug("Saving emotion_category_score: emotion={}, category={}, score={}", score.emotion(), score.category(), score.score());
        return scoreRepository.save(ScoringMapper.toEntity(score))
                .map(ScoringMapper::toDomain);
    }

    @Override
    public Mono<PoiRating> save(PoiRating rating) {
        LOGGER.debug("Saving poi_rating: poiId={}, userId={}, rating={}", rating.poiId(), rating.userId(), rating.rating());
        return ratingRepository.save(ScoringMapper.toEntity(rating))
                .map(ScoringMapper::toDomain);
    }

    @Override
    public Mono<PoiRating> findByUserPoiAndEmotion(Long userId, String poiId, Emotion emotion) {
        LOGGER.debug("Fetching poi_rating: userId={}, poiId={}, emotion={}", userId, poiId, emotion);
        return ratingRepository.findByUserIdAndPoiIdAndEmotion(userId, poiId, emotion.name())
                .map(ScoringMapper::toDomain);
    }
}
