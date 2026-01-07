package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.PoiScore;
import de.tum.moodtrip_backend.core.model.ScoredPoi;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MmrRerankingServiceTest {

    @Test
    void lambdaOneKeepsRelevanceOrder() {
        MmrRerankingService reranker = new MmrRerankingService(1.0, 500, 0.5, 0.4, 0.1, 0);

        ScoredPoi a = poi(1, 10.0, PoiCategory.HISTORY_AND_CULTURE, 0, 0, Map.of("amenity", "museum"));
        ScoredPoi b = poi(2, 8.0, PoiCategory.FOOD_AND_CULINARY, 1, 1, Map.of("amenity", "restaurant"));
        ScoredPoi c = poi(3, 6.0, PoiCategory.NATURE, 2, 2, Map.of("natural", "wood"));

        List<ScoredPoi> reranked = reranker.rerank(List.of(a, b, c), 3);

        assertThat(reranked).containsExactly(a, b, c);
    }

    @Test
    void smallLambdaPromotesCategoryDiversity() {
        MmrRerankingService reranker = new MmrRerankingService(0.2, 500, 0.5, 0.4, 0.1, 0);

        ScoredPoi hist1 = poi(1, 9.0, PoiCategory.HISTORY_AND_CULTURE, 0, 0, Map.of("tourism", "museum"));
        ScoredPoi hist2 = poi(2, 8.0, PoiCategory.HISTORY_AND_CULTURE, 0.01, 0.01, Map.of("historic", "castle"));
        ScoredPoi food1 = poi(3, 7.0, PoiCategory.FOOD_AND_CULINARY, 1, 1, Map.of("amenity", "restaurant"));
        ScoredPoi food2 = poi(4, 6.0, PoiCategory.FOOD_AND_CULINARY, 2, 2, Map.of("amenity", "cafe"));

        List<ScoredPoi> reranked = reranker.rerank(List.of(hist1, hist2, food1, food2), 2);

        assertThat(reranked).containsExactly(hist1, food1);
    }

    @Test
    void geoSimilarityDiscouragesClusteredPois() {
        MmrRerankingService reranker = new MmrRerankingService(0.6, 200, 0.2, 0.7, 0.1, 0);

        ScoredPoi clusterA = poi(1, 9.0, PoiCategory.RELAXATION, 0.0, 0.0, Map.of("leisure", "park"));
        ScoredPoi clusterB = poi(2, 8.0, PoiCategory.RELAXATION, 0.0001, 0.0, Map.of("leisure", "park"));
        ScoredPoi farPoi = poi(3, 7.5, PoiCategory.RELAXATION, 10.0, 10.0, Map.of("leisure", "park"));

        List<ScoredPoi> reranked = reranker.rerank(List.of(clusterA, clusterB, farPoi), 2);

        assertThat(reranked).containsExactly(clusterA, farPoi);
    }

    @Test
    void handlesFlatRelevanceSafely() {
        MmrRerankingService reranker = new MmrRerankingService(0.85, 500, 0.5, 0.4, 0.1, 0);

        ScoredPoi p1 = poi(1, 5.0, PoiCategory.NATURE, 0, 0, Map.of("natural", "wood"));
        ScoredPoi p2 = poi(2, 5.0, PoiCategory.FOOD_AND_CULINARY, 1, 1, Map.of("amenity", "restaurant"));
        ScoredPoi p3 = poi(3, 5.0, PoiCategory.HISTORY_AND_CULTURE, 2, 2, Map.of("tourism", "museum"));

        List<ScoredPoi> reranked = reranker.rerank(List.of(p1, p2, p3), 3);

        assertThat(reranked).containsExactlyInAnyOrder(p1, p2, p3);
    }

    private ScoredPoi poi(long id, double score, PoiCategory category, double lat, double lon, Map<String, String> tags) {
        Poi poi = new Poi(id, Poi.OsmType.NODE, lat, lon, "poi-" + id, category, tags);
        PoiScore poiScore = new PoiScore(score, 0.0, 0.0, 0.0, 0.0, Map.of());
        return new ScoredPoi(poi, poiScore);
    }
}
