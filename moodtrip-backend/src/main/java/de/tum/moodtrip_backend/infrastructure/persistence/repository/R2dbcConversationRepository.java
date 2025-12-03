package de.tum.moodtrip_backend.infrastructure.persistence.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.ConversationEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface R2dbcConversationRepository extends ReactiveCrudRepository<ConversationEntity, Long> {
    @Query("SELECT * FROM conversation WHERE user_id = :userId")
    Flux<ConversationEntity> findByUserId(Long userId);
    
    @Query("SELECT * FROM conversation WHERE user_id = :userId ORDER BY created_at DESC")
    Flux<ConversationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT id FROM conversation WHERE id = :conversationId")
    Mono<Long> findIdById(Long conversationId);
    
    @Query("SELECT COUNT(*) FROM conversation WHERE user_id = :userId")
    Mono<Long> countByUserId(Long userId);
}
