package de.tum.moodtrip_backend.infrastructure.persistence.mapper;

import java.time.LocalDateTime;

import de.tum.moodtrip_backend.core.model.Emotion;
import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.ConversationEntity;
import de.tum.moodtrip_backend.core.model.ConversationDomain;

@Component
public class ConversationMapper {
    
    public ConversationDomain toDomain(ConversationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ConversationDomain(
            entity.getId(),
            entity.getUserId(),
            entity.getTitle(),
            Emotion.fromString(entity.getEmotion()),
            entity.getCreatedAt()
        );
    }
    
    public ConversationEntity toEntity(ConversationDomain domain) {
        if (domain == null) {
            return null;
        }
        String emotionString = (domain.emotion() != null) ? domain.emotion().name() : null;
        return new ConversationEntity(
            domain.id(),
            domain.userId(),
            domain.title(),
            emotionString,
            domain.createdAt() != null ? domain.createdAt() : LocalDateTime.now()
        );
    }
}
