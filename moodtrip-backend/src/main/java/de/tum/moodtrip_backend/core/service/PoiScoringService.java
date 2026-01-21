package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiScore;
import de.tum.moodtrip_backend.core.model.ScoredPoi;
import de.tum.moodtrip_backend.core.model.ScoringConfig;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffset;
import de.tum.moodtrip_backend.core.port.EmotionCategoryScorePort;
import de.tum.moodtrip_backend.core.port.UserPreferenceOffsetPort;
import de.tum.moodtrip_backend.core.util.PoiScoringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PoiScoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoiScoringService.class);
    private static final double DEFAULT_GLOBAL_SCORE = 3.5;

    private final EmotionCategoryScorePort emotionCategoryScorePort;
    private final UserPreferenceOffsetPort userPreferenceOffsetPort;
    private final double shrinkageK;
    private final double lambdaTag;
    private final double lambdaDistance;
    private final double distanceSigmaMeters;
    private final TagWeights tagWeights;
    private final int tagCountMax;
    private final MmrRerankingService mmrRerankingService;
    private final double weightSelectedCategory;

    public PoiScoringService(EmotionCategoryScorePort emotionCategoryScorePort,
                             UserPreferenceOffsetPort userPreferenceOffsetPort,
                             @Value("${app.poi-scoring.shrinkage-k:20.0}") double shrinkageK,
                             @Value("${app.poi-scoring.lambda-tag:0.4}") double lambdaTag,
                             @Value("${app.poi-scoring.lambda-distance:0.25}") double lambdaDistance,
                             @Value("${app.poi-scoring.distance-sigma-meters:1200}") double distanceSigmaMeters,
                             @Value("${app.poi-scoring.tag.weight-name:1.0}") double weightName,
                             @Value("${app.poi-scoring.tag.weight-wikipedia:0.8}") double weightWikipedia,
                             @Value("${app.poi-scoring.tag.weight-wikidata:0.7}") double weightWikidata,
                             @Value("${app.poi-scoring.tag.weight-wikimedia-commons:0.7}") double weightWikimediaCommons,
                             @Value("${app.poi-scoring.tag.weight-website:0.4}") double weightWebsite,
                             @Value("${app.poi-scoring.tag.weight-image:0.6}") double weightImage,
                             @Value("${app.poi-scoring.tag.weight-opening-hours:0.3}") double weightOpeningHours,
                             @Value("${app.poi-scoring.tag.weight-phone:0.3}") double weightPhone,
                             @Value("${app.poi-scoring.tag.weight-tag-count:0.7}") double weightTagCount,
                             @Value("${app.poi-scoring.tag.max-count:20}") int tagCountMax,
                             @Value("${app.poi-scoring.category.weight:1.5}") double weightSelectedCategory,
                             MmrRerankingService mmrRerankingService) {
        this.emotionCategoryScorePort = emotionCategoryScorePort;
        this.userPreferenceOffsetPort = userPreferenceOffsetPort;
        this.shrinkageK = shrinkageK;
        this.lambdaTag = lambdaTag;
        this.lambdaDistance = lambdaDistance;
        this.distanceSigmaMeters = distanceSigmaMeters;
        this.tagWeights = new TagWeights(weightName, weightWikipedia, weightWikidata, weightWikimediaCommons, weightWebsite, weightImage, weightOpeningHours, weightPhone, weightTagCount);
        this.tagCountMax = tagCountMax;
        this.mmrRerankingService = mmrRerankingService;
        this.weightSelectedCategory = weightSelectedCategory;
    }

    public Map<Emotion, Double> normalizeEmotionWeights(Map<Emotion, Double> rawWeights) {
        if (rawWeights == null || rawWeights.isEmpty()) {
            return Map.of(Emotion.NEUTRAL, 1.0);
        }

        double sum = rawWeights.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        if (sum <= 0.0) {
            return Map.of(Emotion.NEUTRAL, 1.0);
        }

        Map<Emotion, Double> normalized = new HashMap<>();
        rawWeights.forEach((emotion, weight) -> {
            if (emotion != null && weight != null && weight > 0) {
                normalized.put(emotion, weight / sum);
            }
        });
        if (normalized.isEmpty()) {
            normalized.put(Emotion.NEUTRAL, 1.0);
        }
        return normalized;
    }



    /**
     * Scores and ranks POIs with custom scoring configuration.
     *
     * @param pois the POIs to score
     * @param userId the user ID for personalization
     * @param rawEmotionWeights raw emotion weights from analysis
     * @param selectedCategories categories selected by user
     * @param originLat origin latitude
     * @param originLon origin longitude
     * @param limit maximum number of POIs to return
     * @param config optional scoring configuration for route type-specific scoring
     * @return scored and ranked POIs
     */
    public Mono<List<ScoredPoi>> scoreAndRank(Flux<Poi> pois,
                                              Long userId,
                                              Map<Emotion, Double> rawEmotionWeights,
                                              List<PoiCategory> selectedCategories,
                                              double originLat,
                                              double originLon,
                                              int limit,
                                              ScoringConfig config) {
        // Apply emotion multiplier from config
        double emotionMultiplier = config != null ? config.emotionMultiplier() : 1.0;
        double categoryBoostMultiplier = config != null ? config.categoryBoostMultiplier() : 1.0;
        double mmrLambda = config != null ? config.mmrLambda() : -1.0; // -1 means use default

        Map<Emotion, Double> emotionWeights = normalizeEmotionWeights(rawEmotionWeights);

        // Apply emotion multiplier to the weights
        Map<Emotion, Double> adjustedEmotionWeights = new HashMap<>();
        for (Map.Entry<Emotion, Double> entry : emotionWeights.entrySet()) {
            adjustedEmotionWeights.put(entry.getKey(), entry.getValue() * emotionMultiplier);
        }
        // Re-normalize after applying multiplier
        Map<Emotion, Double> finalEmotionWeights = normalizeEmotionWeights(adjustedEmotionWeights);

        // Compute effective category boost
        double effectiveCategoryBoost = weightSelectedCategory * categoryBoostMultiplier;

        Mono<Map<PoiCategory, CategoryScore>> categoryScoresMono =
            computeCategoryScoresWithBoost(userId, finalEmotionWeights, selectedCategories, effectiveCategoryBoost);

        final double finalMmrLambda = mmrLambda;
        return categoryScoresMono.flatMap(categoryScores ->
                pois.flatMap(poi -> scorePoi(poi, categoryScores, originLat, originLon))
                        .collectList()
                        .map(candidates -> {
                            List<ScoredPoi> sorted = candidates.stream()
                                    .sorted(Comparator.comparingDouble(sp -> -sp.score().finalScore()))
                                    .toList();
                            if (finalMmrLambda > 0) {
                                return mmrRerankingService.rerankWithLambda(sorted, limit, finalMmrLambda);
                            } else {
                                return mmrRerankingService.rerank(sorted, limit);
                            }
                        })
                        .doOnNext(list -> {
                            String top3 = list.stream()
                                    .limit(3)
                                    .map(p -> String.format("%s(%.2f)", p.poi().name(), p.score().finalScore()))
                                    .collect(Collectors.joining(", "));
                            LOGGER.info("Scoring complete for user {}. Candidates: {}. Top 3: [{}]", userId, list.size(), top3);
                            list.forEach(this::logTopPoi);
                        })
        );
    }

    private Mono<ScoredPoi> scorePoi(Poi poi,
                                     Map<PoiCategory, CategoryScore> categoryScores,
                                     double originLat,
                                     double originLon) {
        CategoryScore categoryScore = categoryScores.get(poi.category());
        if (categoryScore == null) {
            return Mono.empty();
        }

        double tagScore = computeTagScore(poi.tags());
        double distanceMeters = PoiScoringUtils.haversineDistance(originLat, originLon, poi.latitude(), poi.longitude());
        PoiScore poiScore = getPoiScore(distanceMeters, categoryScore, tagScore);

        return Mono.just(new ScoredPoi(poi, poiScore));
    }

    @NonNull
    private PoiScore getPoiScore(double distanceMeters, CategoryScore categoryScore, double tagScore) {
        double distanceScore = lambdaDistance > 0 && distanceSigmaMeters > 0
                ? Math.exp(-distanceMeters / distanceSigmaMeters)
                : 0.0;

        double finalScore = categoryScore.score() + lambdaTag * tagScore + lambdaDistance * distanceScore;
        return new PoiScore(
                finalScore,
                categoryScore.score(),
                categoryScore.baseScore(),
                categoryScore.boostApplied(),
                tagScore,
                distanceScore,
                distanceMeters,
                categoryScore.emotionContributions()
        );
    }

    private Mono<Map<PoiCategory, CategoryScore>> computeCategoryScores(Long userId,
                                                                        Map<Emotion, Double> emotionWeights,
                                                                        List<PoiCategory> selectedCategories) {
        return computeCategoryScoresWithBoost(userId, emotionWeights, selectedCategories, weightSelectedCategory);
    }

    private Mono<Map<PoiCategory, CategoryScore>> computeCategoryScoresWithBoost(Long userId,
                                                                                  Map<Emotion, Double> emotionWeights,
                                                                                  List<PoiCategory> selectedCategories,
                                                                                  double categoryBoost) {
        return Flux.fromArray(PoiCategory.values())
                .flatMap(category -> computeCategoryScoreWithBoost(category, userId, emotionWeights, selectedCategories, categoryBoost))
                .collectMap(CategoryScore::category);
    }

    private Mono<CategoryScore> computeCategoryScore(PoiCategory category,
                                                     Long userId,
                                                     Map<Emotion, Double> emotionWeights,
                                                     List<PoiCategory> selectedCategories) {
        return computeCategoryScoreWithBoost(category, userId, emotionWeights, selectedCategories, weightSelectedCategory);
    }

    private Mono<CategoryScore> computeCategoryScoreWithBoost(PoiCategory category,
                                                               Long userId,
                                                               Map<Emotion, Double> emotionWeights,
                                                               List<PoiCategory> selectedCategories,
                                                               double categoryBoost) {
        return Flux.fromIterable(emotionWeights.entrySet())
                .flatMap(entry -> computeEmotionContribution(category, userId, entry.getKey(), entry.getValue()))
                .collectList()
                .map(contributions -> {
                    double emotionBaseScore = contributions.stream()
                            .mapToDouble(EmotionContribution::weightedScore)
                            .sum();

                    // Intent Confidence Logic:
                    // If the category was explicitly selected in the survey, boost its score multiplicatively.
                    // formula: CategoryScore = EmotionScore * (1 + boost * isSelected)

                    boolean isSelected = selectedCategories != null && selectedCategories.contains(category);
                    double finalCategoryScore = isSelected ? emotionBaseScore * (1 + categoryBoost) : emotionBaseScore;

                    Map<Emotion, Double> byEmotion = contributions.stream()
                            .collect(Collectors.toMap(EmotionContribution::emotion, EmotionContribution::weightedScore));
                    return new CategoryScore(category, finalCategoryScore, emotionBaseScore, isSelected, byEmotion);
                });
    }

    private Mono<EmotionContribution> computeEmotionContribution(PoiCategory category,
                                                                 Long userId,
                                                                 Emotion emotion,
                                                                 double weight) {
        Mono<Double> globalMono = emotionCategoryScorePort.findByEmotionAndCategory(emotion, category)
                .map(score -> score.score() == null ? DEFAULT_GLOBAL_SCORE : score.score())
                .defaultIfEmpty(DEFAULT_GLOBAL_SCORE);
        Mono<UserPreferenceOffset> offsetMono = userPreferenceOffsetPort.findByUserEmotionAndCategory(userId, emotion, category)
                .switchIfEmpty(Mono.just(UserPreferenceOffset.initial(userId, emotion, category, LocalDateTime.now())));

        return Mono.zip(globalMono, offsetMono)
                .map(tuple -> {
                    double globalScore = tuple.getT1();
                    UserPreferenceOffset offset = tuple.getT2();
                    double offsetValue = offset.userPreferenceOffset();
                    long count = offset.count();
                    double alpha = computeAlpha(count);
                    double combined = globalScore + alpha * offsetValue;
                    double weightedScore = weight * combined;
                    return new EmotionContribution(emotion, weightedScore);
                })
                .doOnError(e -> LOGGER.error("Failed to compute emotion contribution for emotion={}, category={}", emotion, category, e))
                .onErrorResume(e -> {
                    LOGGER.warn("Using fallback neutral emotion contribution for emotion={}, category={} due to error", emotion, category, e);
                    return Mono.just(new EmotionContribution(emotion, 0.0));
                });
    }

    private double computeTagScore(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return 0.0;
        }

        boolean hasName = hasNonBlank(tags, "name");
        boolean hasWikipedia = hasNonBlank(tags, "wikipedia");
        boolean hasWikidata = hasNonBlank(tags, "wikidata");
        boolean hasCommons = hasNonBlank(tags, "wikimedia_commons");
        boolean hasWebsite = hasNonBlank(tags, "website");
        boolean hasImage = hasNonBlank(tags, "image");
        boolean hasOpeningHours = hasNonBlank(tags, "opening_hours");
        boolean hasPhone = hasNonBlank(tags, "phone");
        double tagCountNormalized = Math.min(tags.size(), tagCountMax) / (double) tagCountMax;

        double weightedSum =
                tagWeights.name * (hasName ? 1.0 : 0.0) +
                        tagWeights.wikipedia * (hasWikipedia ? 1.0 : 0.0) +
                        tagWeights.wikidata * (hasWikidata ? 1.0 : 0.0) +
                        tagWeights.wikimediaCommons * (hasCommons ? 1.0 : 0.0) +
                        tagWeights.website * (hasWebsite ? 1.0 : 0.0) +
                        tagWeights.image * (hasImage ? 1.0 : 0.0) +
                        tagWeights.openingHours * (hasOpeningHours ? 1.0 : 0.0) +
                        tagWeights.phone * (hasPhone ? 1.0 : 0.0) +
                        tagWeights.tagCount * tagCountNormalized;

        double denominator = tagWeights.totalWeight();
        if (denominator <= 0) {
            return 0.0;
        }
        double normalized = weightedSum / denominator;
        return Math.max(0.0, Math.min(1.0, normalized));
    }

    private boolean hasNonBlank(Map<String, String> tags, String key) {
        String value = tags.get(key);
        return value != null && !value.isBlank();
    }

    private double computeAlpha(long count) {
        return (double) count / (count + shrinkageK);
    }

    private void logTopPoi(ScoredPoi scoredPoi) {
        if (scoredPoi == null || scoredPoi.poi() == null || scoredPoi.score() == null) {
            return;
        }
        Poi poi = scoredPoi.poi();
        PoiScore score = scoredPoi.score();
        Map<String, String> tags = poi.tags() == null ? Map.of() : poi.tags();

        boolean hasWikipedia = hasNonBlank(tags, "wikipedia");
        boolean hasWikidata = hasNonBlank(tags, "wikidata");
        boolean hasCommons = hasNonBlank(tags, "wikimedia_commons");
        boolean hasWebsite = hasNonBlank(tags, "website");
        boolean hasImage = hasNonBlank(tags, "image");
        boolean hasOpeningHours = hasNonBlank(tags, "opening_hours");
        boolean hasPhone = hasNonBlank(tags, "phone");
        boolean hasNameTag = hasNonBlank(tags, "name");
        boolean hasTrustMarker = hasWikipedia || hasWikidata || hasCommons || hasWebsite;

        String emotionBreakdown = score.emotionContributions() == null ? "" :
                score.emotionContributions().entrySet().stream()
                        .sorted(Map.Entry.<Emotion, Double>comparingByValue().reversed())
                        .limit(3)
                        .map(e -> e.getKey() + "=" + String.format("%.3f", e.getValue()))
                        .collect(Collectors.joining(", "));

        LOGGER.debug("Ranked POI -> id={}, name='{}', category={}, finalScore={}, categoryScore={} (base={}, boosted={}), tagScore={}, distanceScore={}, distanceMeters={}, emotions=[{}], signals={{nameTag={}, wikipedia={}, wikidata={}, commons={}, website={}, image={}, opening_hours={}, phone={}, trustMarker={}}}",
                poi.osmId(),
                poi.name(),
                poi.category(),
                String.format("%.3f", score.finalScore()),
                String.format("%.3f", score.categoryScore()),
                String.format("%.3f", score.categoryBaseScore()),
                score.categoryBoostApplied(),
                String.format("%.3f", score.tagScore()),
                String.format("%.3f", score.distanceScore()),
                String.format("%.1f", score.distanceMeters()),
                emotionBreakdown,
                hasNameTag,
                hasWikipedia,
                hasWikidata,
                hasCommons,
                hasWebsite,
                hasImage,
                hasOpeningHours,
                hasPhone,
                hasTrustMarker);
    }

    private record TagWeights(double name,
                              double wikipedia,
                              double wikidata,
                              double wikimediaCommons,
                              double website,
                              double image,
                              double openingHours,
                              double phone,
                              double tagCount) {

        double totalWeight() {
            return name + wikipedia + wikidata + wikimediaCommons + website + image + openingHours + phone + tagCount;
        }
    }

    private record CategoryScore(PoiCategory category,
                                 double score,
                                 double baseScore,
                                 boolean boostApplied,
                                 Map<Emotion, Double> emotionContributions) {
    }

    private record EmotionContribution(Emotion emotion,
                                       double weightedScore) {
    }
}
