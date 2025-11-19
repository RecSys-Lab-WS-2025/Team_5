package de.tum.moodtrip_backend.adapter_database.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import de.tum.moodtrip_backend.adapter_database.entity.MessageEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface R2dbcMessageRepository extends ReactiveCrudRepository<MessageEntity, Long> {
    @Query("SELECT * FROM message WHERE conversation_id = :conversationId")
    Flux<MessageEntity> findByConversationId(Long conversationId);

    @Query("SELECT * FROM message WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    Flux<MessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT * FROM message WHERE sender = :sender")
    Flux<MessageEntity> findBySender(String sender);

    @Query("SELECT COUNT(*) FROM message WHERE conversation_id = :conversationId")
    Mono<Long> countByConversationId(Long conversationId);

    @Query("SELECT * FROM message WHERE conversation_id = :conversationId ORDER BY created_at DESC LIMIT 1")
    Mono<MessageEntity> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}

