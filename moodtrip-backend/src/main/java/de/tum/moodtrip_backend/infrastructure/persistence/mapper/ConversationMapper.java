package de.tum.moodtrip_backend.infrastructure.persistence.mapper;

import java.time.LocalDateTime;

import de.tum.moodtrip_backend.core.model.Emotion;
import de.tum.moodtrip_backend.core.model.EmotionResult;
import org.springframework.stereotype.Component;

import de.tum.moodtrip_backend.infrastructure.persistence.entity.ConversationEntity;
import de.tum.moodtrip_backend.core.model.ConversationDomain;
import de.tum.moodtrip_backend.adapter.content.chatbot.mapper.EmotionMapper;

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
            parseEmotionResult(entity.getEmotionResultJson()),
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
            serializeEmotionResult(domain.emotionResult()),
            domain.createdAt() != null ? domain.createdAt() : LocalDateTime.now()
        );
    }

    private EmotionResult parseEmotionResult(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return EmotionMapper.fromJson(json);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String serializeEmotionResult(EmotionResult result) {
        if (result == null) {
            return null;
        }
        try {
            return EmotionMapper.toJson(result);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
