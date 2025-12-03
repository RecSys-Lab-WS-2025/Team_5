package de.tum.moodtrip_backend.infrastructure.persistence.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.MusicRecommendationEntity;
import de.tum.moodtrip_backend.core.model.MusicRecommendationDomain;

@Component
public class MusicRecommendationMapper {
    
    public MusicRecommendationDomain toDomain(MusicRecommendationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MusicRecommendationDomain(
            entity.getId(),
            entity.getConversationId(),
            entity.getTitle(),
            entity.getLink(),
            entity.getCreatedAt()
        );
    }
    
    public MusicRecommendationEntity toEntity(MusicRecommendationDomain domain) {
        if (domain == null) {
            return null;
        }
        return new MusicRecommendationEntity(
            domain.id(),
            domain.conversationId(),
            domain.title(),
            domain.link(),
            domain.createdAt() != null ? domain.createdAt() : LocalDateTime.now()
        );
    }
}
