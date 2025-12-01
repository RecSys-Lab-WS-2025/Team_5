package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.SurveyDomain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface SurveyPort {
    

    Mono<SurveyDomain> save(SurveyDomain survey);

    Mono<SurveyDomain> findById(Long id);

    Flux<SurveyDomain> findByUserId(Long userId);

    Mono<SurveyDomain> findLatestByUserId(Long userId);

    Flux<SurveyDomain> findByConversationId(Long conversationId);

    Mono<Void> deleteById(Long id);
}
