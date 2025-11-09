package de.tum.moodtrip_backend.repository;

import de.tum.moodtrip_backend.model.Conversation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ConversationRepository extends ReactiveCrudRepository<Conversation, Long> {
    Flux<Conversation> findByUserId(String userId);

}
