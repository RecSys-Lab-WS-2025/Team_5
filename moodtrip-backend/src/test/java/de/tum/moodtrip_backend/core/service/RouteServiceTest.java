package de.tum.moodtrip_backend.core.service;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void returnsFailureWithoutPersistingWhenRouteGenerationErrors() {
        RouteService routeService = new RouteService(
                osmPort,
                wikipediaPort,
                routingPort,
                routeRecommendationPort,
                poiScoringService,
                routeDescriptionService
        );

        when(poiScoringService.scoreAndRank(any(), any(), any(), anyList(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Overpass failure")));
        when(osmPort.findAmenitiesAround(anyDouble(), anyDouble(), any(), anyInt()))
                .thenReturn(Flux.empty());

        // Use specific type parameters
        List<de.tum.moodtrip_backend.core.model.PoiCategory> poiCategories = List.of();
        Map<de.tum.moodtrip_backend.core.model.Emotion, Double> emotionWeights = Map.of();

        StepVerifier.create(routeService.getRoute(1L, 2L, 0.0, 0.0, poiCategories, 1000, emotionWeights, 15, "Test City", 1, false))
                .assertNext(result -> {
                    assertThat(result.status()).isEqualTo(de.tum.moodtrip_backend.core.model.RouteStatus.FAILED);
                    assertThat(result.userMessage()).isNotBlank();
                })
                .verifyComplete();

        verify(routeRecommendationPort, never()).save(any());
    }
}