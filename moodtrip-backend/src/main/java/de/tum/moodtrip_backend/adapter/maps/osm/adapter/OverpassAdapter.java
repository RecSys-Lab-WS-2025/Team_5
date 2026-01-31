package de.tum.moodtrip_backend.adapter.maps.osm.adapter;

import de.tum.moodtrip_backend.adapter.maps.osm.builder.OverpassQueryBuilder;
import de.tum.moodtrip_backend.adapter.maps.osm.mapper.OverpassResponsePOIMapper;
import de.tum.moodtrip_backend.adapter.maps.osm.model.OverpassResponse;
import de.tum.moodtrip_backend.core.eval.EvalRun;
import de.tum.moodtrip_backend.core.exception.MapProviderUnavailableException;
import de.tum.moodtrip_backend.core.model.Poi;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.port.OsmPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Component
public class OverpassAdapter implements OsmPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverpassAdapter.class);
    private final WebClient webClient;

    public OverpassAdapter(Builder webClientBuilder) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        this.webClient = webClientBuilder
                .baseUrl("https://overpass-api.de")
                .exchangeStrategies(strategies)
                .build();
    }

    @Override
    public Flux<Poi> findAmenitiesAround(double lat, double lon, List<PoiCategory> poiCategories, int radiusMeters) {
        return executeQuery(lat, lon, radiusMeters);
    }

    private Flux<Poi> executeQuery(double lat, double lon, int radiusMeters) {
        String query = OverpassQueryBuilder.buildAroundQuery(lat, lon, radiusMeters);
        LOGGER.info("Sending Overpass query around lat={}, lon={}, radius={}", lat, lon, radiusMeters);

        // ✅ MUST be Flux.deferContextual (not Mono.deferContextual), because we finally return Flux<Poi>
        return Flux.deferContextual(ctxView -> {
            EvalRun run = ctxView.hasKey(EvalRun.CTX_KEY) ? ctxView.get(EvalRun.CTX_KEY) : null;

            long t0 = System.nanoTime();
            final long[] rawCountHolder = new long[]{0};

            return webClient.post()
                    .uri("/api/interpreter")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromFormData("data", query))
                    .retrieve()
                    .bodyToMono(OverpassResponse.class)
                    .doOnSuccess(response -> {
                        rawCountHolder[0] = (response != null && response.elements() != null) ? response.elements().size() : 0;
                    })
                    .doOnError(e -> LOGGER.error("Overpass query failed", e))
                    .map(OverpassResponsePOIMapper::toPois)
                    .doOnNext(pois -> {
                        long ms = (System.nanoTime() - t0) / 1_000_000;
                        long raw = rawCountHolder[0];
                        long filtered = pois.size();

                        LOGGER.info("EVAL_OVERPASS rawCount={} trustFilteredCount={} latencyMs={} radiusM={}",
                                raw, filtered, ms, radiusMeters);

                        if (run != null) {
                            run.markOverpass(ms, raw, filtered);
                        }
                    })
                    .flatMapMany(Flux::fromIterable)
                    .retryWhen(Retry.backoff(5, Duration.ofSeconds(1)))
                    .onErrorResume(e -> {
                        LOGGER.error("Overpass unavailable after retries", e);
                        return Flux.error(new MapProviderUnavailableException(e));
                    });
        });
    }
}
