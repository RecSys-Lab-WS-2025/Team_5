package de.tum.moodtrip_backend.adapter_database.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.adapter_database.entity.ConversationEntity;
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
            entity.getEmotion(),
            entity.getCreatedAt()
        );
    }
    
    public ConversationEntity toEntity(ConversationDomain domain) {
        if (domain == null) {
            return null;
        }
        return new ConversationEntity(
            domain.id(),
            domain.userId(),
            domain.title(),
            domain.emotion(),
            domain.createdAt() != null ? domain.createdAt() : LocalDateTime.now()
        );
    }
}
