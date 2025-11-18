package de.tum.moodtrip_backend.adapter_database.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.adapter_database.entity.MessageEntity;
import de.tum.moodtrip_backend.core.model.MessageDomain;

@Component
public class MessageMapper {
    
    public MessageDomain toDomain(MessageEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MessageDomain(
            entity.getId(),
            entity.getConversationId(),
            entity.getSender(),
            entity.getContent(),
            entity.getCreatedAt()
        );
    }
    
    public MessageEntity toEntity(MessageDomain domain) {
        if (domain == null) {
            return null;
        }
        return new MessageEntity(
            domain.id(),
            domain.conversationId(),
            domain.sender(),
            domain.content(),
            domain.timestamp() != null ? domain.timestamp() : LocalDateTime.now()
        );
    }
}
