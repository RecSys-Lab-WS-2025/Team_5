package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffset;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffsetUpdateResult;
import de.tum.moodtrip_backend.core.port.GlobalMappingRepository;
import de.tum.moodtrip_backend.core.port.UserPreferenceOffsetPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class UserPreferenceOffsetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserPreferenceOffsetService.class);

    private final UserPreferenceOffsetPort userPreferenceOffsetPort;
    private final GlobalMappingRepository globalMappingRepository;
    private final TransactionalOperator transactionalOperator;
    private final double learningRate;
    private final double regularization;
    private final double shrinkageK;
    private final double offsetMin;
    private final double offsetMax;

    public UserPreferenceOffsetService(UserPreferenceOffsetPort userPreferenceOffsetPort,
                                       GlobalMappingRepository globalMappingRepository,
                                       TransactionalOperator transactionalOperator,
                                       @Value("${app.user-preference-offset.learning-rate:0.05}") double learningRate,
                                       @Value("${app.user-preference-offset.regularization:0.001}") double regularization,
                                       @Value("${app.user-preference-offset.shrinkage-k:20.0}") double shrinkageK,
                                       @Value("${app.user-preference-offset.offset-min:-2.0}") double offsetMin,
                                       @Value("${app.user-preference-offset.offset-max:2.0}") double offsetMax) {
        this.userPreferenceOffsetPort = userPreferenceOffsetPort;
        this.globalMappingRepository = globalMappingRepository;
        this.transactionalOperator = transactionalOperator;
        this.learningRate = learningRate;
        this.regularization = regularization;
        this.shrinkageK = shrinkageK;
        this.offsetMin = offsetMin;
        this.offsetMax = offsetMax;
    }

    public Mono<UserPreferenceOffsetUpdateResult> updateUserPreferenceOffset(Long userId, Emotion emotion, PoiCategory category, double rating, boolean incrementCount) {
        if (rating < 0.5 || rating > 5.0) {
            return Mono.error(new IllegalArgumentException("Rating must be between 0.5 and 5"));
        }

        return transactionalOperator.transactional(
                globalMappingRepository.getScore(emotion, category)
                        .switchIfEmpty(Mono.error(new IllegalStateException("Global mapping missing for " + emotion + "/" + category)))
                        .flatMap(globalScore -> applyUpdate(userId, emotion, category, rating, globalScore, incrementCount))
        );
    }

    private Mono<UserPreferenceOffsetUpdateResult> applyUpdate(Long userId, Emotion emotion, PoiCategory category, double rating, double globalScore, boolean incrementCount) {
        return userPreferenceOffsetPort.findByUserEmotionAndCategoryForUpdate(userId, emotion, category)
                .flatMap(existing -> updateExistingOffset(existing, rating, globalScore, incrementCount))
                .switchIfEmpty(Mono.defer(() -> insertFirstOffset(userId, emotion, category, rating, globalScore, incrementCount)));
    }

    private Mono<UserPreferenceOffsetUpdateResult> insertFirstOffset(Long userId, Emotion emotion, PoiCategory category, double rating, double globalScore, boolean incrementCount) {
        LocalDateTime now = LocalDateTime.now();
        UpdateComputation computation = computeNext(UserPreferenceOffset.initial(userId, emotion, category, now), rating, globalScore, now, incrementCount);

        return userPreferenceOffsetPort.insertIfAbsent(computation.updatedOffset())
                .flatMap(saved -> Mono.just(buildResult(saved, computation.predicted(), computation.error())))
                .switchIfEmpty(Mono.defer(() -> {
                    // Another transaction may have inserted concurrently; retry with the latest row locked.
                    return userPreferenceOffsetPort.findByUserEmotionAndCategoryForUpdate(userId, emotion, category)
                            .flatMap(existing -> updateExistingOffset(existing, rating, globalScore, incrementCount))
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "Failed to insert or find UserPreferenceOffset for userId=%d, emotion=%s, category=%s"
                                            .formatted(userId, emotion, category))));
                }));
    }

    private Mono<UserPreferenceOffsetUpdateResult> updateExistingOffset(UserPreferenceOffset existing, double rating, double globalScore, boolean incrementCount) {
        LocalDateTime now = LocalDateTime.now();
        UpdateComputation computation = computeNext(existing.withUpdatedAt(now), rating, globalScore, now, incrementCount);

        return userPreferenceOffsetPort.upsert(computation.updatedOffset())
                .map(saved -> buildResult(saved, computation.predicted(), computation.error()));
    }

    private UserPreferenceOffsetUpdateResult buildResult(UserPreferenceOffset offset, double predicted, double error) {
        double alphaAfter = computeAlpha(offset.count());
        LOGGER.debug("Updated offset for user={}, emotion={}, category={}, offset={}, count={}, alpha={}",
                offset.userId(), offset.emotion(), offset.category(), offset.userPreferenceOffset(), offset.count(), alphaAfter);
        return new UserPreferenceOffsetUpdateResult(offset, alphaAfter, predicted, error);
    }

    private double computeAlpha(long count) {
        return count / (count + shrinkageK);
    }

    private double clampOffset(double value) {
        return Math.max(offsetMin, Math.min(offsetMax, value));
    }

    private UpdateComputation computeNext(UserPreferenceOffset current,
                                          double rating,
                                          double globalScore,
                                          LocalDateTime now,
                                          boolean incrementCount) {
        // Use the current interaction count to compute alpha for prediction.
        // The predicted rating is the global baseline score plus a user-specific offset.
        double alphaPred = computeAlpha(current.count());
        double predicted = globalScore + alphaPred * current.userPreferenceOffset();
        // Error is the residual between the observed rating and the current prediction.
        double error = rating - predicted;

        long newCount = current.count() + (incrementCount ? 1 : 0);
        // For the update we recompute alpha based on the (possibly) incremented count
        // so that the step size can depend on how many observations we have.
        double alphaUpdate = computeAlpha(newCount);

        double delta = current.userPreferenceOffset();
        // Gradient-like update of the user preference offset:
        //   alphaUpdate * error     -> error-driven term (pushes offset to better fit the rating)
        //   regularization * delta  -> L2-style shrinkage (pulls offset back towards 0 to avoid overfitting)
        // The net update is scaled by the global learningRate.
        double updatedDelta = delta + learningRate * (alphaUpdate * error - regularization * delta);
        // Ensure the learned offset stays within the configured [offsetMin, offsetMax] range.
        updatedDelta = clampOffset(updatedDelta);

        UserPreferenceOffset updated = new UserPreferenceOffset(
                current.id(),
                current.userId(),
                current.emotion(),
                current.category(),
                updatedDelta,
                newCount,
                now
        );

        return new UpdateComputation(updated, predicted, error);
    }


    private record UpdateComputation(UserPreferenceOffset updatedOffset, double predicted, double error) {
    }
}
