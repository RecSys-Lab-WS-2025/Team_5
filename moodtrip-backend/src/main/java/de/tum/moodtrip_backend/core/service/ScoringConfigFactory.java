package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.RouteType;
import de.tum.moodtrip_backend.core.model.ScoringConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Factory for creating ScoringConfig instances based on application properties.
 */
@Service
public class ScoringConfigFactory {

    // Balanced route config
    @Value("${app.route-type.balanced.emotion-multiplier:1.0}")
    private double balancedEmotionMultiplier;
    @Value("${app.route-type.balanced.category-boost-multiplier:0.5}")
    private double balancedCategoryBoostMultiplier;
    @Value("${app.route-type.balanced.mmr-lambda:0.6}")
    private double balancedMmrLambda;

    // Your Picks route config
    @Value("${app.route-type.your-picks.emotion-multiplier:1.0}")
    private double yourPicksEmotionMultiplier;
    @Value("${app.route-type.your-picks.category-boost-multiplier:3.0}")
    private double yourPicksCategoryBoostMultiplier;
    @Value("${app.route-type.your-picks.mmr-lambda:1.0}")
    private double yourPicksMmrLambda;

    // Discovery route config
    @Value("${app.route-type.discovery.emotion-multiplier:1.0}")
    private double discoveryEmotionMultiplier;
    @Value("${app.route-type.discovery.category-boost-multiplier:0.3}")
    private double discoveryCategoryBoostMultiplier;
    @Value("${app.route-type.discovery.mmr-lambda:0.50}")
    private double discoveryMmrLambda;

    public ScoringConfig balanced() {
        return new ScoringConfig(
            RouteType.BALANCED,
            balancedEmotionMultiplier,
            balancedCategoryBoostMultiplier,
            balancedMmrLambda
        );
    }

    public ScoringConfig yourPicks() {
        return new ScoringConfig(
            RouteType.YOUR_PICKS,
            yourPicksEmotionMultiplier,
            yourPicksCategoryBoostMultiplier,
            yourPicksMmrLambda
        );
    }

    public ScoringConfig discovery() {
        return new ScoringConfig(
            RouteType.DISCOVERY,
            discoveryEmotionMultiplier,
            discoveryCategoryBoostMultiplier,
            discoveryMmrLambda
        );
    }

    public ScoringConfig[] allConfigs() {
        return new ScoringConfig[] {
            balanced(),
            yourPicks(),
            discovery()
        };
    }

}
