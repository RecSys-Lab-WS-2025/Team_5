package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.PoiRating;
import reactor.core.publisher.Mono;

public interface PoiRatingPort {
    Mono<PoiRating> save(PoiRating rating);
    Mono<PoiRating> findByUserPoiAndEmotion(Long userId, String poiId, Emotion emotion);
}
