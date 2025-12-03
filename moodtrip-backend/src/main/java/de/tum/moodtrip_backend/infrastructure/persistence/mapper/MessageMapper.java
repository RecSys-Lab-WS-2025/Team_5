package de.tum.moodtrip_backend.infrastructure.persistence.mapper;

import java.time.LocalDateTime;

import de.tum.moodtrip_backend.core.model.Sender;
import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.MessageEntity;
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
                Sender.fromString(entity.getSender()),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }

    public MessageEntity toEntity(MessageDomain domain) {
        if (domain == null) {
            return null;
        }
        String senderString = (domain.sender() != null) ? domain.sender().name() : null;
        return new MessageEntity(
                domain.id(),
                domain.conversationId(),
                senderString,
                domain.content(),
                domain.timestamp() != null ? domain.timestamp() : LocalDateTime.now()
        );
    }
}
