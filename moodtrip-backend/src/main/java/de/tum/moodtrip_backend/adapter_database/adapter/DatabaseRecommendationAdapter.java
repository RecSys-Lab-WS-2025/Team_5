package de.tum.moodtrip_backend.adapter_database.adapter;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseRecommendationAdapter.class);

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

        var sql = databaseClient.sql(
                "INSERT INTO recommendation (conversation_id, type, title, description, link, created_at, track_id, route_data) " +
                        "VALUES (:conversationId, :type, :title, :description, :link, :createdAt, :trackId, CAST(:routeData AS jsonb)) " +
                        "RETURNING id, conversation_id, type, title, description, link, created_at, track_id, route_data"
        );
        // Handle nullable conversationId
        if (entity.getConversationId() != null) {
            sql = sql.bind("conversationId", entity.getConversationId());
        } else {
            sql = sql.bindNull("conversationId", Long.class);
        }
        // Handle nullable type
        sql = sql.bind("type", entity.getType() != null ? entity.getType() : "");
        // Handle nullable title
        sql = sql.bind("title", entity.getTitle() != null ? entity.getTitle() : "");
        // Handle nullable description
        sql = sql.bind("description", entity.getDescription() != null ? entity.getDescription() : "");
        // Handle nullable link
        sql = sql.bind("link", entity.getLink() != null ? entity.getLink() : "");
        // Handle nullable createdAt
        sql = sql.bind("createdAt", entity.getCreatedAt());
        // Handle nullable trackId
        if (entity.getTrackId() != null) {
            sql = sql.bind("trackId", entity.getTrackId());
        } else {
            sql = sql.bindNull("trackId", String.class);
        }

        if (entity.getRouteData() != null && !entity.getRouteData().isBlank()) {
            sql = sql.bind("routeData", entity.getRouteData());
        } else {
            sql = sql.bindNull("routeData", String.class);
        }

        return sql.fetch()
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
                    if (trackIdObj != null) {
                        String trackIdStr = trackIdObj.toString();
                        saved.setTrackId(!trackIdStr.isEmpty() ? trackIdStr : null);
                    } else {
                        saved.setTrackId(null);
                    }
                    Object routeDataObj = row.get("route_data");
                    if (routeDataObj != null) {
                        String jsonStr = extractJsonb(routeDataObj);
                        saved.setRouteData(jsonStr);
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

    /**
     * Robust JSONB extraction from PostgreSQL R2DBC row result.
     * Handles multiple possible return types (String, CharSequence, byte[], ByteBuf, Json wrapper).
     */
    private String extractJsonb(Object obj) {
        // Preferred: direct String/CharSequence
        if (obj instanceof CharSequence) {
            String s = obj.toString();
            return s.isBlank() ? null : s;
        }

        // Byte array payload
        if (obj instanceof byte[]) {
            String s = new String((byte[]) obj, StandardCharsets.UTF_8);
            return s.isBlank() ? null : s;
        }

        // PostgreSQL driver's Json type - try to get string representation without reflection
        String className = obj.getClass().getName();
        if (className.startsWith("io.r2dbc.postgresql.codec.Json")) {
            // The toString() method of Json types returns the JSON content
            String s = obj.toString();
            return (s == null || s.isBlank()) ? null : s;
        }

        // Optional: Netty ByteBuf (some R2DBC drivers may return this)
        try {
            Class<?> byteBufClass = Class.forName("io.netty.buffer.ByteBuf");
            if (byteBufClass.isInstance(obj)) {
                try {
                    java.lang.reflect.Method toStringMethod = byteBufClass.getMethod("toString", java.nio.charset.Charset.class);
                    String s = (String) toStringMethod.invoke(obj, StandardCharsets.UTF_8);
                    return (s == null || s.isBlank()) ? null : s;
                } catch (Exception ignored) {
                    // Continue to next fallback
                }
            }
        } catch (ClassNotFoundException ignored) {
            // Netty ByteBuf not available, skip
        }

        // Last resort: generic toString with logging
        try {
            LOG.debug("Extracting JSONB using toString() for type: {}", obj.getClass().getName());
            String s = obj.toString();
            // Remove known wrapper pattern if present (shouldn't happen with direct toString approach)
            if (s.startsWith("JsonByteArrayInput{") && s.endsWith("}")) {
                s = s.substring("JsonByteArrayInput{".length(), s.length() - 1);
            }
            return s.isBlank() ? null : s;
        } catch (Exception e) {
            LOG.error("Failed to extract JSONB using toString() for type: {}", obj.getClass().getName(), e);
            return null;
        }
    }
}
