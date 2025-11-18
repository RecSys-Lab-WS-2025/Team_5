package de.tum.moodtrip_backend.adapter_database.adapter;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.adapter_database.entity.RecommendationEntity;
import de.tum.moodtrip_backend.adapter_database.mapper.RecommendationMapper;
import de.tum.moodtrip_backend.adapter_database.repository.R2dbcRecommendationRepository;
import de.tum.moodtrip_backend.core.model.RecommendationDomain;
import de.tum.moodtrip_backend.core.port.RecommendationPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DatabaseRecommendationAdapter implements RecommendationPort {
    
    private final R2dbcRecommendationRepository recommendationRepository;
    private final RecommendationMapper recommendationMapper;
    private final DatabaseClient databaseClient;

    public DatabaseRecommendationAdapter(R2dbcRecommendationRepository recommendationRepository,
                                         RecommendationMapper recommendationMapper,
                                         DatabaseClient databaseClient) {
        this.recommendationRepository = recommendationRepository;
        this.recommendationMapper = recommendationMapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<RecommendationDomain> save(RecommendationDomain recommendation) {
        RecommendationEntity entity = recommendationMapper.toEntity(recommendation);
        
        return databaseClient.sql(
                "INSERT INTO recommendation (conversation_id, type, title, description, link, created_at, track_id, route_data) " +
                "VALUES (:conversationId, :type, :title, :description, :link, :createdAt, :trackId, CAST(:routeData AS jsonb)) " +
                "RETURNING id, conversation_id, type, title, description, link, created_at, track_id, route_data"
        )
        .bind("conversationId", entity.getConversationId())
        .bind("type", entity.getType())
        .bind("title", entity.getTitle() != null ? entity.getTitle() : "")
        .bind("description", entity.getDescription() != null ? entity.getDescription() : "")
        .bind("link", entity.getLink() != null ? entity.getLink() : "")
        .bind("createdAt", entity.getCreatedAt())
        .bind("trackId", entity.getTrackId() != null ? entity.getTrackId() : "")
        .bind("routeData", entity.getRouteData() != null ? entity.getRouteData() : "")
        .fetch()
        .first()
        .map(row -> {
            RecommendationEntity saved = new RecommendationEntity();
            saved.setId((Long) row.get("id"));
            saved.setConversationId((Long) row.get("conversation_id"));
            saved.setType((String) row.get("type"));
            saved.setTitle((String) row.get("title"));
            saved.setDescription((String) row.get("description"));
            saved.setLink((String) row.get("link"));
            
            Object createdAtObj = row.get("created_at");
            if (createdAtObj instanceof java.time.OffsetDateTime) {
                saved.setCreatedAt(((java.time.OffsetDateTime) createdAtObj).toLocalDateTime());
            } else if (createdAtObj instanceof java.time.LocalDateTime) {
                saved.setCreatedAt((java.time.LocalDateTime) createdAtObj);
            }
            
            Object trackIdObj = row.get("track_id");
            saved.setTrackId(trackIdObj != null && !trackIdObj.toString().isEmpty() ? trackIdObj.toString() : null);
            
            Object routeDataObj = row.get("route_data");
            if (routeDataObj != null) {
                try {
                    // PostgreSQL JSONB returns as Json object, call asString() via reflection
                    java.lang.reflect.Method asStringMethod = routeDataObj.getClass().getMethod("asString");
                    String jsonStr = (String) asStringMethod.invoke(routeDataObj);
                    saved.setRouteData(jsonStr != null && !jsonStr.isEmpty() ? jsonStr : null);
                } catch (Exception e) {
                    // Fallback: remove wrapper if present
                    String routeStr = routeDataObj.toString();
                    if (routeStr.startsWith("JsonByteArrayInput{") && routeStr.endsWith("}")) {
                        routeStr = routeStr.substring("JsonByteArrayInput{".length(), routeStr.length() - 1);
                    }
                    saved.setRouteData(!routeStr.isEmpty() ? routeStr : null);
                }
            }
            
            return saved;
        })
        .map(recommendationMapper::toDomain);
    }

    @Override
    public Mono<RecommendationDomain> findById(Long id) {
        return recommendationRepository.findById(id)
                .map(recommendationMapper::toDomain);
    }

    @Override
    public Flux<RecommendationDomain> findByConversationId(Long conversationId) {
        return recommendationRepository.findByConversationIdOrderByCreatedAtDesc(conversationId)
                .map(recommendationMapper::toDomain);
    }
    
    @Override
    public Flux<RecommendationDomain> findByConversationIdAndType(Long conversationId, String type) {
        return recommendationRepository.findByConversationIdAndType(conversationId, type)
                .map(recommendationMapper::toDomain);
    }

    @Override
    public Flux<RecommendationDomain> findByType(String type) {
        return recommendationRepository.findByType(type)
                .map(recommendationMapper::toDomain);
    }

    @Override
    public Mono<Long> countByConversationId(Long conversationId) {
        return recommendationRepository.countByConversationId(conversationId);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return recommendationRepository.deleteById(id);
    }
}
