package de.tum.moodtrip_backend.adapter_osm.adapter;

import de.tum.moodtrip_backend.adapter_osm.mapper.OverpassResponsePOIMapper;
import de.tum.moodtrip_backend.adapter_osm.mapper.OverpassResponseRouteMapper;
import de.tum.moodtrip_backend.adapter_osm.builder.POICategoryOsmQueryBuilder;
import de.tum.moodtrip_backend.adapter_osm.model.OverpassResponse;
import de.tum.moodtrip_backend.core.model.POI;
import de.tum.moodtrip_backend.core.model.POICategory;
import de.tum.moodtrip_backend.core.model.Route;
import de.tum.moodtrip_backend.core.port.OsmPort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OverpassAdapter implements OsmPort {

    private final WebClient webClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(OverpassAdapter.class);

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
    public Mono<Route> fetchRelationWithWays(long relationId) {
        String q = """
                [out:json][timeout:60];
                relation(%d);
                way(r)->.rways;
                ( .rways; node(w.rways); );
                out geom;
                """.formatted(relationId);

        LOGGER.info("[Overpass] fetchRelationWithWays: relationId={}", relationId);

        return webClient.post()
                .uri("/api/interpreter")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData("data", q))
                .retrieve()
                .bodyToMono(OverpassResponse.class)
                .map(overpassResponse -> OverpassResponseRouteMapper.toDomain(overpassResponse, relationId));
    }

    @Override
    public Mono<Long> findAmenitiesAround(double lat, double lon, String routeType, long radiusMeters) {
        String q = """
                [out:json][timeout:60];
                rel(around:%d,%f,%f)
                  ["type"="route"]["route"~"^(%s)$"];
                out tags center 1;
                """.formatted(radiusMeters, lat, lon, routeType);

        LOGGER.info("[Overpass] findAmenitiesAround: lat={}, lon={}, type={}, radius={}m", lat, lon, routeType, radiusMeters);

        return webClient.post()
                .uri("/api/interpreter")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData("data", q))
                .retrieve()
                .bodyToMono(OverpassResponse.class)
                .flatMap(resp -> {
                    int n = resp != null && resp.elements() != null ? resp.elements().size() : 0;
                    LOGGER.info("[Overpass] <- search response OK (elements={})", n);
                    if (n == 0) {
                        String msg = String.format("No matching route relations found within %dm (lat=%.6f, lon=%.6f, type=%s)",
                                radiusMeters, lat, lon, routeType);
                        LOGGER.warn("[Overpass] {}", msg);
                        return Mono.error(new java.util.NoSuchElementException(msg));
                    }
                    Long id = resp.elements().getFirst().id();
                    LOGGER.info("[Overpass] picked relationId={} from search results", id);
                    return Mono.just(id);
                });
    }

    @Override
    public Flux<POI> findAmenitiesAround(double lat, double lon, POICategory poiCategory, int radiusMeters) {
        String query = POICategoryOsmQueryBuilder.buildAroundQuery(poiCategory, lat, lon, radiusMeters);
        return webClient.post()
                .uri("/api/interpreter")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData("data", query))
                .retrieve()
                .bodyToMono(OverpassResponse.class)
                .map(OverpassResponsePOIMapper::toPois)
                .flatMapMany(Flux::fromIterable);
    }
}