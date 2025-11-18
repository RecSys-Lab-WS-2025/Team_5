package de.tum.moodtrip_backend.adapter_database.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import de.tum.moodtrip_backend.adapter_database.entity.ConversationEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface R2dbcConversationRepository extends ReactiveCrudRepository<ConversationEntity, Long> {
    @Query("SELECT * FROM conversation WHERE user_id = :userId")
    Flux<ConversationEntity> findByUserId(String userId);
    
    @Query("SELECT * FROM conversation WHERE user_id = :userId ORDER BY created_at DESC")
    Flux<ConversationEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    
    @Query("SELECT * FROM conversation WHERE emotion = :emotion")
    Flux<ConversationEntity> findByEmotion(String emotion);
    
    @Query("SELECT COUNT(*) FROM conversation WHERE user_id = :userId")
    Mono<Long> countByUserId(String userId);
}
