package de.tum.moodtrip_backend.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.infrastructure.persistence.mapper.ConversationMapper;
import de.tum.moodtrip_backend.infrastructure.persistence.mapper.MessageMapper;
import de.tum.moodtrip_backend.infrastructure.persistence.repository.R2dbcConversationRepository;
import de.tum.moodtrip_backend.infrastructure.persistence.repository.R2dbcMessageRepository;
import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.core.model.MessageDomain;
import de.tum.moodtrip_backend.core.port.ConversationPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DatabaseConversationAdapter implements ConversationPort {

    private final R2dbcConversationRepository conversationRepository;
    private final R2dbcMessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public DatabaseConversationAdapter(R2dbcConversationRepository conversationRepository,
                                       R2dbcMessageRepository messageRepository,
                                       ConversationMapper conversationMapper,
                                       MessageMapper messageMapper) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public Mono<ConversationDomain> save(ConversationDomain conversation) {
        return Mono.just(conversation)
                .map(conversationMapper::toEntity)
                .flatMap(conversationRepository::save)
                .map(conversationMapper::toDomain);
    }

    @Override
    public Mono<ConversationDomain> findById(Long id) {
        return conversationRepository.findById(id)
                .map(conversationMapper::toDomain);
    }

    @Override
    public Flux<ConversationDomain> findByUserId(Long userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .map(conversationMapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return conversationRepository.deleteById(id);
    }

    @Override
    public Mono<MessageDomain> saveMessage(MessageDomain message) {
        return Mono.just(message)
                .map(messageMapper::toEntity)
                .flatMap(messageRepository::save)
                .map(messageMapper::toDomain);
    }

    @Override
    public Flux<MessageDomain> findMessagesByConversationId(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .map(messageMapper::toDomain);
    }

    @Override
    public Mono<Long> countMessagesByConversationId(Long conversationId) {
        return messageRepository.countByConversationId(conversationId);
    }

    /**
     * Delete all messages belonging to the given conversation.
     */
    @Override
    public Mono<Void> deleteMessagesByConversationId(Long conversationId) {
        return messageRepository.deleteByConversationId(conversationId);
    }
}
