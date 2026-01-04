package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import reactor.core.publisher.Mono;

public interface GlobalMappingRepository {
    Mono<Double> getScore(Emotion emotion, PoiCategory category);
}
