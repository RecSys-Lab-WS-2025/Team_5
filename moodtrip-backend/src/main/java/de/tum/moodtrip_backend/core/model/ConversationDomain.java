package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

public record ConversationDomain(
    Long id,
    String userId,
    String title,
    String emotion,
    LocalDateTime createdAt
) {
    public ConversationDomain withId(Long id) {
        return new ConversationDomain(id, userId, title, emotion, createdAt);
    }
    
    public ConversationDomain withEmotion(String emotion) {
        return new ConversationDomain(id, userId, title, emotion, createdAt);
    }
    public ConversationDomain withTitle(String title) {
        return new ConversationDomain(id, userId, title, emotion, createdAt);
    }
}
