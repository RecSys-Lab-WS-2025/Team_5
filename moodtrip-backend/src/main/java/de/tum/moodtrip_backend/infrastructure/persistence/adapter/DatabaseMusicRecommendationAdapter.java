package de.tum.moodtrip_backend.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.infrastructure.persistence.mapper.MusicRecommendationMapper;
import de.tum.moodtrip_backend.infrastructure.persistence.repository.R2dbcMusicRecommendationRepository;
import de.tum.moodtrip_backend.core.model.MusicRecommendationDomain;
import de.tum.moodtrip_backend.core.port.MusicRecommendationPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DatabaseMusicRecommendationAdapter implements MusicRecommendationPort {

    private final R2dbcMusicRecommendationRepository musicRecommendationRepository;
    private final MusicRecommendationMapper musicRecommendationMapper;

    public DatabaseMusicRecommendationAdapter(R2dbcMusicRecommendationRepository musicRecommendationRepository,
                                              MusicRecommendationMapper musicRecommendationMapper) {
        this.musicRecommendationRepository = musicRecommendationRepository;
        this.musicRecommendationMapper = musicRecommendationMapper;
    }

    @Override
    public Mono<MusicRecommendationDomain> save(MusicRecommendationDomain musicRecommendation) {
        return Mono.just(musicRecommendation)
                .map(musicRecommendationMapper::toEntity)
                .flatMap(musicRecommendationRepository::save)
                .map(musicRecommendationMapper::toDomain);
    }

    @Override
    public Mono<MusicRecommendationDomain> findById(Long id) {
        return musicRecommendationRepository.findById(id)
                .map(musicRecommendationMapper::toDomain);
    }

    @Override
    public Flux<MusicRecommendationDomain> findByConversationId(Long conversationId) {
        return musicRecommendationRepository.findByConversationIdOrderByCreatedAtDesc(conversationId)
                .map(musicRecommendationMapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return musicRecommendationRepository.deleteById(id);
    }
}
