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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OverpassAdapter implements OsmPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverpassAdapter.class);
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
    public Flux<Poi> findAmenitiesAround(double lat, double lon, List<PoiCategory> poiCategories, int radiusMeters) {
        return Flux.fromIterable(poiCategories)
                .flatMap(category -> {
                    String query = POICategoryOsmQueryBuilder.buildAroundQuery(category, lat, lon, radiusMeters);
                    LOGGER.info("Sending Overpass query for category {}: {}", category, query);

                    return webClient.post()
                            .uri("/api/interpreter")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromFormData("data", query))
                            .retrieve()
                            .bodyToMono(OverpassResponse.class)
                            .doOnSuccess(response -> {
                                if (response != null && response.elements() != null) {
                                    LOGGER.info("Overpass returned {} elements for category {}", response.elements().size(), category);
                                } else {
                                    LOGGER.info("Overpass returned empty response for category {}", category);
                                }
                            })
                            .doOnError(e -> LOGGER.error("Overpass query failed for category {}", category, e))
                            .map(response -> OverpassResponsePOIMapper.toPois(response, category))
                            .flatMapMany(Flux::fromIterable);
                })
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(10)));
    }
}