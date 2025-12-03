package de.tum.moodtrip_backend.adapter.maps.osm.adapter;

import de.tum.moodtrip_backend.adapter.maps.osm.mapper.OverpassResponsePOIMapper;
import de.tum.moodtrip_backend.adapter.maps.osm.builder.POICategoryOsmQueryBuilder;
import de.tum.moodtrip_backend.adapter.maps.osm.model.OverpassResponse;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.port.OsmPort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class OverpassAdapter implements OsmPort {

    private final WebClient webClient;

    public OverpassAdapter(Builder webClientBuilder) {

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10 MB
                .build();

        this.webClient = webClientBuilder
                .baseUrl("https://overpass-api.de")
                .exchangeStrategies(strategies)
                .build();
    }

    @Override
    public Flux<Poi> findAmenitiesAround(double lat, double lon, PoiCategory poiCategory, int radiusMeters) {
        String query = POICategoryOsmQueryBuilder.buildAroundQuery(poiCategory, lat, lon, radiusMeters);
        return webClient.post()
                .uri("/api/interpreter")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData("data", query))
                .retrieve()
                .bodyToMono(OverpassResponse.class)
                .map(OverpassResponsePOIMapper::toPois)
                .flatMapMany(Flux::fromIterable)
                .retryWhen(Retry.fixedDelay(5, Duration.ofMillis(10)));
    }
}