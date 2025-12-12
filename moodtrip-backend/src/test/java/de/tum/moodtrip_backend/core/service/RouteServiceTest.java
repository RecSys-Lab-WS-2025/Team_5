package de.tum.moodtrip_backend.core.service;

import de.tum.moodtrip_backend.core.port.OsmPort;
import de.tum.moodtrip_backend.core.port.RouteRecommendationPort;
import de.tum.moodtrip_backend.core.port.RoutingPort;
import de.tum.moodtrip_backend.core.port.WikipediaPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void returnsFailureWithoutPersistingWhenRouteGenerationErrors() {
        RouteService routeService = new RouteService(
                osmPort,
                wikipediaPort,
                routingPort,
                routeRecommendationPort
        );

        when(osmPort.findAmenitiesAround(anyDouble(), anyDouble(), anyList(), anyInt()))
                .thenReturn(Flux.error(new RuntimeException("Overpass failure")));

        StepVerifier.create(routeService.getRoute(1L, 0.0, 0.0, List.of(), 1000))
                .assertNext(result -> {
                    assertThat(result.status()).isEqualTo(de.tum.moodtrip_backend.core.model.RouteStatus.FAILED);
                    assertThat(result.userMessage()).isNotBlank();
                })
                .verifyComplete();

        verify(routeRecommendationPort, never()).save(any());
    }
}
