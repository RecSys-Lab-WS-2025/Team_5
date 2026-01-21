package de.tum.moodtrip_backend.core.model;

/**
 * Configuration for POI scoring parameters based on route type.
 * Values are populated from application properties via ScoringConfigFactory.
 *
 * @param routeType the type of route being generated
 * @param emotionMultiplier multiplier applied to emotion weights (higher = more emotion influence)
 * @param categoryBoostMultiplier multiplier for category boost (higher = more category influence)
 * @param mmrLambda MMR lambda for relevance-diversity trade-off (higher = more relevance)
 */
public record ScoringConfig(
        RouteType routeType,
        double emotionMultiplier,
        double categoryBoostMultiplier,
        double mmrLambda
) {
}
