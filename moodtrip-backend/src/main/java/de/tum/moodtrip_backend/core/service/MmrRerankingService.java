package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.ScoredPoi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies Maximal Marginal Relevance (MMR) to re-rank POIs for diversity.
 */
@Component
public class MmrRerankingService {

    private static final Set<String> CORE_TAG_KEYS = Set.of("amenity", "tourism", "historic", "leisure", "shop", "sport", "natural");

    private final double lambda;
    private final double geoSigmaMeters;
    private final double wCat;
    private final double wGeo;
    private final double wTag;
    private final int maxPerCategory;

    public MmrRerankingService(@Value("${app.poi-scoring.mmr.lambda:0.85}") double lambda,
                               @Value("${app.poi-scoring.mmr.geo-sigma-meters:500}") double geoSigmaMeters,
                               @Value("${app.poi-scoring.mmr.w-cat:0.5}") double wCat,
                               @Value("${app.poi-scoring.mmr.w-geo:0.4}") double wGeo,
                               @Value("${app.poi-scoring.mmr.w-tag:0.1}") double wTag,
                               @Value("${app.poi-scoring.mmr.max-per-category:0}") int maxPerCategory) {
        this.lambda = clamp01(lambda);
        this.geoSigmaMeters = geoSigmaMeters;
        this.wCat = wCat;
        this.wGeo = wGeo;
        this.wTag = wTag;
        this.maxPerCategory = maxPerCategory;
    }

    public List<ScoredPoi> rerank(List<ScoredPoi> candidates, int k) {
        if (candidates == null || candidates.isEmpty() || k <= 0) {
            return List.of();
        }

        int targetK = Math.min(k, candidates.size());

        double minRel = candidates.stream().mapToDouble(sp -> sp.score().finalScore()).min().orElse(0.0);
        double maxRel = candidates.stream().mapToDouble(sp -> sp.score().finalScore()).max().orElse(minRel);
        boolean flat = Double.compare(minRel, maxRel) == 0;

        List<ScoredPoi> remaining = new ArrayList<>(candidates);
        List<ScoredPoi> selected = new ArrayList<>();
        Map<PoiCategory, Integer> categoryCounts = new HashMap<>();

        while (selected.size() < targetK && !remaining.isEmpty()) {
            ScoredPoi best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (ScoredPoi candidate : remaining) {
                if (maxPerCategory > 0) {
                    int count = categoryCounts.getOrDefault(candidate.poi().category(), 0);
                    if (count >= maxPerCategory) {
                        continue;
                    }
                }

                double relNorm = flat ? 1.0 : normalize(candidate.score().finalScore(), minRel, maxRel);
                double maxSim = selected.isEmpty() ? 0.0 : selected.stream()
                        .mapToDouble(sel -> similarity(candidate.poi(), sel.poi()))
                        .max()
                        .orElse(0.0);

                double mmr = lambda * relNorm - (1 - lambda) * maxSim;
                if (mmr > bestScore) {
                    bestScore = mmr;
                    best = candidate;
                }
            }

            if (best == null) {
                break;
            }
            selected.add(best);
            remaining.remove(best);
            categoryCounts.merge(best.poi().category(), 1, Integer::sum);
        }

        return selected;
    }

    private double similarity(Poi a, Poi b) {
        double totalWeight = wCat + wGeo + wTag;
        if (totalWeight <= 0) {
            return 0.0;
        }

        double simCat = (a.category() == b.category()) ? 1.0 : 0.0;
        double simGeo = geoSimilarity(a, b);
        double simTag = tagSimilarity(a, b);

        double combined = (wCat * simCat) + (wGeo * simGeo) + (wTag * simTag);
        double normalized = combined / totalWeight;
        return clamp01(normalized);
    }

    private double geoSimilarity(Poi a, Poi b) {
        if (geoSigmaMeters <= 0) {
            return 0.0;
        }
        if (!hasValidCoords(a) || !hasValidCoords(b)) {
            return 0.0;
        }
        double distMeters = haversineDistance(a.latitude(), a.longitude(), b.latitude(), b.longitude());
        return Math.exp(-distMeters / geoSigmaMeters);
    }

    private boolean hasValidCoords(Poi poi) {
        return poi != null
                && !Double.isNaN(poi.latitude()) && !Double.isInfinite(poi.latitude())
                && !Double.isNaN(poi.longitude()) && !Double.isInfinite(poi.longitude());
    }

    private double tagSimilarity(Poi a, Poi b) {
        Map<String, String> tagsA = a.tags();
        Map<String, String> tagsB = b.tags();
        if (tagsA == null || tagsB == null || tagsA.isEmpty() || tagsB.isEmpty()) {
            return 0.0;
        }

        Set<String> setA = extractCoreTagKeys(tagsA);
        Set<String> setB = extractCoreTagKeys(tagsB);

        if (setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return (double) intersection.size() / union.size();
    }

    private Set<String> extractCoreTagKeys(Map<String, String> tags) {
        Set<String> result = new HashSet<>();
        for (String key : CORE_TAG_KEYS) {
            String val = tags.get(key);
            if (val != null && !val.isBlank()) {
                result.add(key);
            }
        }
        return result;
    }

    private double normalize(double value, double min, double max) {
        if (Double.compare(min, max) == 0) {
            return 1.0;
        }
        return (value - min) / (max - min);
    }

    private double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
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
}
