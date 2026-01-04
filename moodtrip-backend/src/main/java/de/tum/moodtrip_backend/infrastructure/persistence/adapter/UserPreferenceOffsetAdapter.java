package de.tum.moodtrip_backend.infrastructure.persistence.adapter;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.PoiCategory;
import de.tum.moodtrip_backend.core.model.UserPreferenceOffset;
import de.tum.moodtrip_backend.core.port.UserPreferenceOffsetPort;
import de.tum.moodtrip_backend.infrastructure.persistence.mapper.UserPreferenceOffsetMapper;
import de.tum.moodtrip_backend.infrastructure.persistence.repository.R2dbcUserPreferenceOffsetRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserPreferenceOffsetAdapter implements UserPreferenceOffsetPort {

    private final R2dbcUserPreferenceOffsetRepository repository;

    public UserPreferenceOffsetAdapter(R2dbcUserPreferenceOffsetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<UserPreferenceOffset> findByUserEmotionAndCategory(Long userId, Emotion emotion, PoiCategory category) {
        return repository.findByUserIdAndEmotionAndCategory(userId, emotion.name(), category.name())
                .map(UserPreferenceOffsetMapper::toDomain);
    }

    @Override
    public Mono<UserPreferenceOffset> findByUserEmotionAndCategoryForUpdate(Long userId, Emotion emotion, PoiCategory category) {
        return repository.findByUserIdAndEmotionAndCategory(userId, emotion.name(), category.name())
                .map(UserPreferenceOffsetMapper::toDomain);
    }

    @Override
    public Mono<UserPreferenceOffset> insertIfAbsent(UserPreferenceOffset offset) {
        return repository.findByUserIdAndEmotionAndCategory(offset.userId(), offset.emotion().name(), offset.category().name())
                .flatMap(existing -> Mono.just(UserPreferenceOffsetMapper.toDomain(existing)))
                .switchIfEmpty(repository.save(UserPreferenceOffsetMapper.toEntity(offset)).map(UserPreferenceOffsetMapper::toDomain));
    }

    @Override
    public Mono<UserPreferenceOffset> upsert(UserPreferenceOffset offset) {
        return repository.save(UserPreferenceOffsetMapper.toEntity(offset))
                .map(UserPreferenceOffsetMapper::toDomain);
    }
}
