package de.tum.moodtrip_backend.repository;

import de.tum.moodtrip_backend.model.Message;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message,Long> {
    Flux<Message> findByConversationId(Long conversationId);
}
