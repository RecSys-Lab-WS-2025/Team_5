package de.tum.moodtrip_backend.core.service.evaluation;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffset;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffsetUpdateResult;
import de.tum.moodtrip_backend.core.port.UserPreferenceOffsetPort;
import de.tum.moodtrip_backend.core.service.UserPreferenceOffsetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simulates the personalization feedback loop (EQ3) to verify Empirical Bayes shrinkage.
 * Scenario:
 * - User Emotion: ENERGIZED
 * - Target Category: HISTORY_AND_CULTURE
 * - Global Score: 1.0 (Low baseline)
 * - User Preference: 5.0 (High personal interest)
 * - Goal: Observe Delta (offset) drifting from 0.0 towards +4.0 over 50 rounds.
 */
class PersonalizationConvergenceEvaluationTest {

    @Test
    @DisplayName("Simulate 50 rounds of user feedback and verify personalization drift")
    void simulatePersonalizationLoop() {
        // 1. Setup
        InMemoryUserPreferenceOffsetPort mockPort = new InMemoryUserPreferenceOffsetPort();
        
        // Config: learningRate=0.1, regularization=0.01, shrinkageK=10, min=-5, max=5
        UserPreferenceOffsetService service = new UserPreferenceOffsetService(
                mockPort,
                0.1,    // learningRate (eta_p)
                0.01,   // regularization (lambda_reg)
                10.0,   // shrinkageK
                -5.0,   // offsetMin
                5.0     // offsetMax
        );

        Long userId = 123L;
        Emotion emotion = Emotion.ENERGIZED;
        PoiCategory category = PoiCategory.HISTORY_AND_CULTURE;
        double targetRating = 5.0;
        double currentGlobalScore = 1.0;
        double globalLearningRate = 0.01;

        System.out.println("Round, GlobalPrior, UserPref, Delta(Offset), Alpha(Confidence), Predicted, ActualRating, Error");

        // 2. Simulation Loop
        double lastDelta = 0.0;
        double lastError = 0.0;

        for (int round = 1; round <= 50; round++) {
            // Act: Update preference based on user giving a 5.0 rating
            UserPreferenceOffsetUpdateResult result = service.updateUserPreferenceOffsetWithGlobal(
                    userId, emotion, category, targetRating, currentGlobalScore, true
            ).block();

            assertThat(result).isNotNull();
            
            // Log for visualization
            UserPreferenceOffset updated = result.offset();
            double delta = updated.userPreferenceOffset();
            double alpha = result.alpha();
            double predicted = result.predictedScore();
            double error = result.error();

            System.out.printf("%d, %.4f, %.1f, %.4f, %.4f, %.4f, %.1f, %.4f%n", 
                    round, currentGlobalScore, targetRating, delta, alpha, predicted, targetRating, error);

            // Simulate Global Prior Update (as done in PoiRatingService)
            // wNew = wOld + globalLearningRate * error
            currentGlobalScore += globalLearningRate * error;
            // Clamp to [0, 5]
            currentGlobalScore = Math.max(0.0, Math.min(5.0, currentGlobalScore));

            // Assertions for trend
            if (round > 1) {
                // Delta should generally increase (drifting positive)
                assertThat(delta).describedAs("Delta should grow towards positive").isGreaterThanOrEqualTo(lastDelta - 0.01); 
            }
            
            lastDelta = delta;
            lastError = Math.abs(error);
        }

        // 3. Final Verification
        // After 50 rounds, the model should have adapted significantly.
        // Theoretically: Predicted ~= Global(1.0) + Alpha(~0.8) * Delta
        // We expect Predicted to be close to 5.0
        
        assertThat(lastDelta).describedAs("Final offset should be significantly positive").isGreaterThan(3.0);
        assertThat(lastError).describedAs("Prediction error should be small after convergence").isLessThan(1.0);
    }



    /**
     * In-memory implementation of the port for testing purposes.
     */
    static class InMemoryUserPreferenceOffsetPort implements UserPreferenceOffsetPort {
        private final Map<String, UserPreferenceOffset> storage = new ConcurrentHashMap<>();

        private String key(Long userId, Emotion emotion, PoiCategory category) {
            return userId + ":" + emotion + ":" + category;
        }

        @Override
        public Mono<UserPreferenceOffset> findByUserEmotionAndCategory(Long userId, Emotion emotion, PoiCategory category) {
            return Mono.justOrEmpty(storage.get(key(userId, emotion, category)));
        }

        @Override
        public Mono<UserPreferenceOffset> findByUserEmotionAndCategoryForUpdate(Long userId, Emotion emotion, PoiCategory category) {
            // For simulation, we don't need row locking, just return the obj
            return findByUserEmotionAndCategory(userId, emotion, category);
        }

        @Override
        public Mono<UserPreferenceOffset> insertIfAbsent(UserPreferenceOffset offset) {
            String k = key(offset.userId(), offset.emotion(), offset.category());
            UserPreferenceOffset existing = storage.putIfAbsent(k, offset);
            return existing == null ? Mono.just(offset) : Mono.empty();
        }

        @Override
        public Mono<UserPreferenceOffset> upsert(UserPreferenceOffset offset) {
            String k = key(offset.userId(), offset.emotion(), offset.category());
            storage.put(k, offset);
            return Mono.just(offset);
        }
    }
}
