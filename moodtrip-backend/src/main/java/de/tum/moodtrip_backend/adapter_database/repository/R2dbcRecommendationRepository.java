package de.tum.moodtrip_backend.adapter_database.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import de.tum.moodtrip_backend.adapter_database.entity.RecommendationEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface R2dbcRecommendationRepository extends ReactiveCrudRepository<RecommendationEntity, Long> {
    @Query("SELECT * FROM recommendation WHERE conversation_id = :conversationId")
    Flux<RecommendationEntity> findByConversationId(Long conversationId);
    
    @Query("SELECT * FROM recommendation WHERE conversation_id = :conversationId AND type = :type")
    Flux<RecommendationEntity> findByConversationIdAndType(Long conversationId, String type);
    
    @Query("SELECT * FROM recommendation WHERE type = :type")
    Flux<RecommendationEntity> findByType(String type);
    
    @Query("SELECT * FROM recommendation WHERE conversation_id = :conversationId ORDER BY created_at DESC")
    Flux<RecommendationEntity> findByConversationIdOrderByCreatedAtDesc(Long conversationId);
    
    @Query("SELECT COUNT(*) FROM recommendation WHERE conversation_id = :conversationId")
    Mono<Long> countByConversationId(Long conversationId);
    
    @Query("SELECT COUNT(*) FROM recommendation WHERE conversation_id = :conversationId AND type = :type")
    Mono<Long> countByConversationIdAndType(Long conversationId, String type);
}
