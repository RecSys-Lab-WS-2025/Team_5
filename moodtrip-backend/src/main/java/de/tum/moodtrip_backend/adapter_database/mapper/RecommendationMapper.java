package de.tum.moodtrip_backend.adapter_database.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.adapter_database.entity.RecommendationEntity;
import de.tum.moodtrip_backend.core.model.RecommendationDomain;

@Component
public class RecommendationMapper {
    
    public RecommendationDomain toDomain(RecommendationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RecommendationDomain(
            entity.getId(),
            entity.getConversationId(),
            entity.getType(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getLink(),
            entity.getTrackId(),
            entity.getRouteData(),
            entity.getCreatedAt()
        );
    }
    
    public RecommendationEntity toEntity(RecommendationDomain domain) {
        if (domain == null) {
            return null;
        }
        return new RecommendationEntity(
            domain.id(),
            domain.conversationId(),
            domain.type(),
            domain.title(),
            domain.description(),
            domain.link(),
            domain.trackId(),
            domain.routeData(),
            domain.createdAt() != null ? domain.createdAt() : LocalDateTime.now()
        );
    }
}
