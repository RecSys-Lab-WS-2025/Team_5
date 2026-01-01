package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.PoiRatingEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface R2dbcPoiRatingRepository extends ReactiveCrudRepository<PoiRatingEntity, Long> {
    Mono<PoiRatingEntity> findByUserIdAndPoiIdAndEmotion(Long userId, String poiId, String emotion);
}
