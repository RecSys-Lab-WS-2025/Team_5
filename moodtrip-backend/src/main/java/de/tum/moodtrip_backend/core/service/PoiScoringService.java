package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiScore;
import de.tum.moodtrip_backend.core.model.ScoredPoi;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffset;
import de.tum.moodtrip_backend.core.port.GlobalMappingRepository;
import de.tum.moodtrip_backend.core.port.UserPreferenceOffsetPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private final GlobalMappingRepository globalMappingRepository;
    private final UserPreferenceOffsetPort userPreferenceOffsetPort;
    private final double shrinkageK;
    private final double lambdaTag;
    private final double lambdaDistance;
    private final double distanceSigmaMeters;
    private final TagWeights tagWeights;
    private final int tagCountMax;

    public PoiScoringService(GlobalMappingRepository globalMappingRepository,
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
                             @Value("${app.poi-scoring.tag.max-count:20}") int tagCountMax) {
        this.globalMappingRepository = globalMappingRepository;
        this.userPreferenceOffsetPort = userPreferenceOffsetPort;
        this.shrinkageK = shrinkageK;
        this.lambdaTag = lambdaTag;
        this.lambdaDistance = lambdaDistance;
        this.distanceSigmaMeters = distanceSigmaMeters;
        this.tagWeights = new TagWeights(weightName, weightWikipedia, weightWikidata, weightWikimediaCommons, weightWebsite, weightImage, weightOpeningHours, weightPhone, weightTagCount);
        this.tagCountMax = tagCountMax;
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

    public Mono<List<ScoredPoi>> scoreAndRank(Flux<Poi> pois,
                                              Long userId,
                                              Map<Emotion, Double> rawEmotionWeights,
                                              double originLat,
                                              double originLon,
                                              int limit) {
        Map<Emotion, Double> emotionWeights = normalizeEmotionWeights(rawEmotionWeights);

        Mono<Map<PoiCategory, CategoryScore>> categoryScoresMono = computeCategoryScores(userId, emotionWeights);

        return categoryScoresMono.flatMap(categoryScores ->
                pois.flatMap(poi -> scorePoi(poi, categoryScores, originLat, originLon))
                        .collectList()
                        .map(list -> list.stream()
                                .sorted(Comparator.comparingDouble(sp -> -sp.score().finalScore()))
                                .limit(limit)
                                .toList())
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
        double distanceMeters = haversineDistance(originLat, originLon, poi.latitude(), poi.longitude());
        double distanceScore = lambdaDistance > 0 && distanceSigmaMeters > 0
                ? Math.exp(-distanceMeters / distanceSigmaMeters)
                : 0.0;

        double finalScore = categoryScore.score() + lambdaTag * tagScore + lambdaDistance * distanceScore;
        PoiScore poiScore = new PoiScore(
                finalScore,
                categoryScore.score(),
                tagScore,
                distanceScore,
                distanceMeters,
                categoryScore.emotionContributions()
        );

        return Mono.just(new ScoredPoi(poi, poiScore));
    }

    private Mono<Map<PoiCategory, CategoryScore>> computeCategoryScores(Long userId,
                                                                        Map<Emotion, Double> emotionWeights) {
        return Flux.fromArray(PoiCategory.values())
                .flatMap(category -> computeCategoryScore(category, userId, emotionWeights))
                .collectMap(CategoryScore::category);
    }

    private Mono<CategoryScore> computeCategoryScore(PoiCategory category,
                                                     Long userId,
                                                     Map<Emotion, Double> emotionWeights) {
        return Flux.fromIterable(emotionWeights.entrySet())
                .flatMap(entry -> computeEmotionContribution(category, userId, entry.getKey(), entry.getValue()))
                .collectList()
                .map(contributions -> {
                    double totalScore = contributions.stream()
                            .mapToDouble(EmotionContribution::weightedScore)
                            .sum();
                    Map<Emotion, Double> byEmotion = contributions.stream()
                            .collect(Collectors.toMap(EmotionContribution::emotion, EmotionContribution::weightedScore));
                    return new CategoryScore(category, totalScore, byEmotion);
                });
    }

    private Mono<EmotionContribution> computeEmotionContribution(PoiCategory category,
                                                                 Long userId,
                                                                 Emotion emotion,
                                                                 double weight) {
        Mono<Double> globalMono = globalMappingRepository.getScore(emotion, category);
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

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
                                 Map<Emotion, Double> emotionContributions) {
    }

    private record EmotionContribution(Emotion emotion,
                                       double weightedScore) {
    }
}
