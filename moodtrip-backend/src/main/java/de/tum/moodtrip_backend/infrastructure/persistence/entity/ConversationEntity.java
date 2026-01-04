package de.tum.moodtrip_backend.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("conversation")
public class ConversationEntity {
    @Id
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    private String title;
    private String emotion;
    @Column("emotion_result_json")
    private String emotionResultJson;
    
    @Column("created_at")
    private LocalDateTime createdAt;

    public ConversationEntity() {
    }

    public ConversationEntity(Long id, Long userId, String title, String emotion, String emotionResultJson, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.emotion = emotion;
        this.emotionResultJson = emotionResultJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public String getEmotionResultJson() {
        return emotionResultJson;
    }

    public void setEmotionResultJson(String emotionResultJson) {
        this.emotionResultJson = emotionResultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
