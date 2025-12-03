package de.tum.moodtrip_backend.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.infrastructure.persistence.mapper.SurveyMapper;
import de.tum.moodtrip_backend.infrastructure.persistence.repository.R2dbcSurveyRepository;
import de.tum.moodtrip_backend.core.model.SurveyDomain;
import de.tum.moodtrip_backend.core.port.SurveyPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



@Component
public class DatabaseSurveyAdapter implements SurveyPort {
    
    private final R2dbcSurveyRepository repository;
    private final SurveyMapper mapper;
    
    public DatabaseSurveyAdapter(final R2dbcSurveyRepository repository, final SurveyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
    
    @Override
    public Mono<SurveyDomain> save(SurveyDomain survey) {
        return Mono.just(survey)
                .map(mapper::toEntity)
                .flatMap(repository::save)
                .map(mapper::toDomain);
    }
    
    @Override
    public Mono<SurveyDomain> findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<SurveyDomain> findByConversationId(Long conversationId) {
        return repository.findByConversationId(conversationId)
                .map(mapper::toDomain);
    }
    
    @Override
    public Flux<SurveyDomain> findByUserId(Long userId) {
        return repository.findByUserId(userId)
                .map(mapper::toDomain);
    }
    
    @Override
    public Mono<SurveyDomain> findLatestByUserId(Long userId) {
        return repository.findLatestByUserId(userId)
                .map(mapper::toDomain);
    }
    
    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }
}
