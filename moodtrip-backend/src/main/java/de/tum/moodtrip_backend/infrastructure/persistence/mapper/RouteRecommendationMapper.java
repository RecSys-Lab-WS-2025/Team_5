package de.tum.moodtrip_backend.infrastructure.persistence.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.RouteRecommendationEntity;
import de.tum.moodtrip_backend.core.model.RouteRecommendationDomain;
import io.r2dbc.postgresql.codec.Json;

@Component
public class RouteRecommendationMapper {
    
    private final ObjectMapper objectMapper;

    public RouteRecommendationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    // DB (Json) -> Domain (JsonNode)
    public RouteRecommendationDomain toDomain(RouteRecommendationEntity entity) {
        if (entity == null) {
            return null;
        }
        
        JsonNode routeDataNode = null;
        if (entity.getRouteData() != null) {
            try {
                routeDataNode = objectMapper.readTree(entity.getRouteData().asString());
            } catch (Exception e) {
                throw new RuntimeException("Error parsing JSON from database", e);
            }
        }
        
        return new RouteRecommendationDomain(
            entity.getId(),
            entity.getConversationId(),
            routeDataNode,
            entity.getCreatedAt()
        );
    }

    public RouteRecommendationEntity toEntity(RouteRecommendationDomain domain) {
        if (domain == null) {
            return null;
        }
        
        Json routeDataJson = null;
        if (domain.routeData() != null) {
            try {
                String jsonString = objectMapper.writeValueAsString(domain.routeData());
                routeDataJson = Json.of(jsonString);
            } catch (Exception e) {
                throw new RuntimeException("Error serializing JSON for database", e);
            }
        }
        
        return new RouteRecommendationEntity(
            domain.id(),
            domain.conversationId(),
            routeDataJson,
            domain.createdAt() != null ? domain.createdAt() : LocalDateTime.now()
        );
    }
}