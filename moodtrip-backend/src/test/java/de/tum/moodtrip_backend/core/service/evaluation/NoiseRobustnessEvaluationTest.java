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

/**
 * Evaluation Test: Noise Robustness
 * Compares Empirical Bayes (Adaptive Alpha) vs Over-Confident (Fixed High Alpha).
 * Scenario:
 * - User gives consistent high ratings (5.0) for 10 rounds.
 * - Round 11: User gives a sudden low rating (1.0) - "Noise" or "Anomaly".
 * - Goal: Demonstrate that EB is robust (small drop) while Fixed Alpha overreacts (large drop).
 */
public class NoiseRobustnessEvaluationTest {

    @Test
    @DisplayName("Compare EB vs Over-Confident (Alpha=0.9) under Noise")
    void evaluateNoiseRobustness() {
        // Setup EB Service
        InMemoryUserPreferenceOffsetPort portEB = new InMemoryUserPreferenceOffsetPort();
        UserPreferenceOffsetService serviceEB = new UserPreferenceOffsetService(portEB, 0.1, 0.01, 10.0, -5.0, 5.0);

        // Setup Over-Confident Service (Fixed Alpha = 0.9)
        InMemoryUserPreferenceOffsetPort portHigh = new InMemoryUserPreferenceOffsetPort();
        UserPreferenceOffsetService serviceHigh = new UserPreferenceOffsetService(portHigh, 0.1, 0.01, 10.0, -5.0, 5.0) {
            @Override
            protected double computeAlpha(long count) {
                return 0.9; // Fixed High Confidence
            }
        };

        // Shared Context
        Long userId = 123L;
        Emotion emotion = Emotion.ENERGIZED;
        PoiCategory category = PoiCategory.HISTORY_AND_CULTURE;
        double globalScoreEB = 1.0;
        double globalScoreHigh = 1.0;
        double globalLearningRate = 0.01;

        System.out.println("Round, Rating, Pred_EB, Alpha_EB, Pred_High, Alpha_High");

        // Run for 15 rounds: 10 consistent, 1 noise, 4 recovery
        for (int round = 1; round <= 15; round++) {
            // Noise Injection: Round 11 is a 1.0 rating, others are 5.0
            double rating = (round == 11) ? 1.0 : 5.0;

            // 1. Run EB
            UserPreferenceOffsetUpdateResult resEB = serviceEB.updateUserPreferenceOffsetWithGlobal(
                    userId, emotion, category, rating, globalScoreEB, true
            ).block();
            globalScoreEB += globalLearningRate * resEB.error();
            globalScoreEB = clamp(globalScoreEB);

            // 2. Run High Alpha
            UserPreferenceOffsetUpdateResult resHigh = serviceHigh.updateUserPreferenceOffsetWithGlobal(
                    userId, emotion, category, rating, globalScoreHigh, true
            ).block();
            globalScoreHigh += globalLearningRate * resHigh.error();
            globalScoreHigh = clamp(globalScoreHigh);
            
            System.out.printf("%d, %.1f, %.4f, %.4f, %.4f, %.4f%n", 
                    round, rating, resEB.predictedScore(), resEB.alpha(), resHigh.predictedScore(), resHigh.alpha());
        }
    }

    private double clamp(double v) { return Math.max(0.0, Math.min(5.0, v)); }

    // Reused Mock Port
    static class InMemoryUserPreferenceOffsetPort implements UserPreferenceOffsetPort {
        private final Map<String, UserPreferenceOffset> storage = new ConcurrentHashMap<>();
        private String key(Long u, Emotion e, PoiCategory c) { return u + ":" + e + ":" + c; }
        public Mono<UserPreferenceOffset> findByUserEmotionAndCategory(Long u, Emotion e, PoiCategory c) { return Mono.justOrEmpty(storage.get(key(u, e, c))); }
        public Mono<UserPreferenceOffset> findByUserEmotionAndCategoryForUpdate(Long u, Emotion e, PoiCategory c) { return findByUserEmotionAndCategory(u,e,c); }
        public Mono<UserPreferenceOffset> insertIfAbsent(UserPreferenceOffset o) {
            return storage.putIfAbsent(key(o.userId(), o.emotion(), o.category()), o) == null ? Mono.just(o) : Mono.empty();
        }
        public Mono<UserPreferenceOffset> upsert(UserPreferenceOffset o) {
            storage.put(key(o.userId(), o.emotion(), o.category()), o); return Mono.just(o);
        }
    }
}
