package de.tum.moodtrip_backend.core.service;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.model.RouteGenerationResult;
import de.tum.moodtrip_backend.core.model.RouteText;
import de.tum.moodtrip_backend.core.model.RouteType;
import de.tum.moodtrip_backend.core.model.ScoredPoi;
import de.tum.moodtrip_backend.core.model.ScoringConfig;
import de.tum.moodtrip_backend.core.port.OsmPort;
import de.tum.moodtrip_backend.core.port.RouteRecommendationPort;
import de.tum.moodtrip_backend.core.port.RoutingPort;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private OsmPort osmPort;

    @Mock
    private WikipediaPort wikipediaPort;

    @Mock
    private RoutingPort routingPort;

    @Mock
    private RouteRecommendationPort routeRecommendationPort;

    @Mock
    private PoiScoringService poiScoringService;

    @Mock
    private RouteDescriptionService routeDescriptionService;

    @Mock
    private ScoringConfigFactory scoringConfigFactory;

    @InjectMocks
    private RouteService routeService;

    @Test
    @DisplayName("Should generate three routes (Balanced, Emotion, Category) successfully")
    void shouldGenerateThreeRoutesSuccessfully() {
        // Given
        long conversationId = 1L;
        long userId = 100L;
        double lat = 48.1351;
        double lon = 11.5820;
        List<PoiCategory> categories = List.of(PoiCategory.HISTORY_AND_CULTURE);
        Map<Emotion, Double> emotionWeights = Map.of(Emotion.JOYFUL, 1.0);
        int radius = 1000;
        int poiLimit = 10;
        String city = "Munich";
        int tripDays = 1;

        // Mock generic POI data
        Poi poi1 = new Poi(1L, Poi.OsmType.NODE, 48.1, 11.5, "poi1", PoiCategory.HISTORY_AND_CULTURE, Map.of("name", "Museum"));
        Poi poi2 = new Poi(2L, Poi.OsmType.NODE, 48.2, 11.6, "poi2", PoiCategory.FOOD_AND_CULINARY, Map.of("name", "Restaurant"));
        Flux<Poi> poiFlux = Flux.just(poi1, poi2);

        // Mock OSM Port
        when(osmPort.findAmenitiesAround(anyDouble(), anyDouble(), anyList(), anyInt()))
                .thenReturn(poiFlux);

        // Mock Config Factory to return 3 configs
        ScoringConfig[] configs = new ScoringConfig[]{
                new ScoringConfig(RouteType.YOUR_PICKS, 1.0, 0.5, 0.5),
                new ScoringConfig(RouteType.DISCOVERY, 1.0, 0.5, 0.5),
                new ScoringConfig(RouteType.BALANCED, 1.0, 0.5, 0.5)
        };
        when(scoringConfigFactory.allConfigs()).thenReturn(configs);

        // Mock Scoring Service
        ScoredPoi sp1 = new ScoredPoi(poi1, null);
        ScoredPoi sp2 = new ScoredPoi(poi2, null);
        when(poiScoringService.scoreAndRank(any(), anyLong(), anyMap(), anyList(), anyDouble(), anyDouble(), anyInt(), any()))
                .thenReturn(Mono.just(List.of(sp1, sp2)));

        // Mock Enrichment (Wikipedia)
        when(wikipediaPort.fetchSummaryForTag(any())).thenReturn(Mono.just("Summary"));
        when(wikipediaPort.fetchImageUrl(any(), any(), any(), any())).thenReturn(Mono.just("http://image.url"));

        // Mock Routing Port
        Route mockRoute = new Route(1000.0, 600.0, List.of(), List.of(), List.of(), List.of(0, 1), null, null);
        when(routingPort.calculateRoute(anyList())).thenReturn(Mono.just(mockRoute));

        // Mock Description Service
        RouteText routeText = new RouteText("Test Title", Map.of(1, "Day 1 Description"));
        when(routeDescriptionService.generateBatchRouteText(any(), any(), anyList(), anyBoolean()))
                .thenReturn(Mono.just(Map.of(
                        RouteType.YOUR_PICKS, routeText,
                        RouteType.DISCOVERY, routeText,
                        RouteType.BALANCED, routeText
                )));

        // Mock Saving
        when(routeRecommendationPort.save(any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        // When
        Mono<List<RouteGenerationResult>> resultMono = routeService.getMultipleRoutes(
                conversationId, userId, lat, lon, categories, radius, emotionWeights, poiLimit, city, tripDays, false
        );

        // Then
        StepVerifier.create(resultMono)
                .assertNext(results -> {
                    assertThat(results).hasSize(3);
                    assertThat(results.get(0).routeType()).isEqualTo(RouteType.BALANCED);
                    assertThat(results.get(1).routeType()).isEqualTo(RouteType.YOUR_PICKS);
                    assertThat(results.get(2).routeType()).isEqualTo(RouteType.DISCOVERY);
                })
                .verifyComplete();
    }
}