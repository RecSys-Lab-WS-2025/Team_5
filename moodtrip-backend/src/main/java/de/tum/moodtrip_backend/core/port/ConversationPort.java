package de.tum.moodtrip_backend.core.port;

import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConversationPort {

    Mono<ConversationDomain> save(ConversationDomain conversation);

    Mono<ConversationDomain> findById(Long id);

    Flux<ConversationDomain> findByUserId(Long userId);

    Mono<Void> deleteById(Long id);

    Mono<MessageDomain> saveMessage(MessageDomain message);

    Flux<MessageDomain> findMessagesByConversationId(Long conversationId);

    Mono<Long> countMessagesByConversationId(Long conversationId);

    /**
     * Delete all messages belonging to the given conversation.
     */
    Mono<Void> deleteMessagesByConversationId(Long conversationId);
}
