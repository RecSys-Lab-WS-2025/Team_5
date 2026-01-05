package de.tum.moodtrip_backend.core.model;

import java.time.LocalDateTime;

public record ConversationDomain(
    Long id,
    Long userId,
    String title,
    Emotion emotion,
    EmotionResult emotionResult,
    LocalDateTime createdAt
) {
    public ConversationDomain withId(Long id) {
        return new ConversationDomain(id, userId, title, emotion, emotionResult, createdAt);
    }
    
    public ConversationDomain withEmotion(Emotion emotion) {
        return new ConversationDomain(id, userId, title, emotion, emotionResult, createdAt);
    }
    public ConversationDomain withTitle(String title) {
        return new ConversationDomain(id, userId, title, emotion, emotionResult, createdAt);
    }
    public ConversationDomain withEmotionResult(EmotionResult emotionResult) {
        return new ConversationDomain(id, userId, title, emotion, emotionResult, createdAt);
    }
}
