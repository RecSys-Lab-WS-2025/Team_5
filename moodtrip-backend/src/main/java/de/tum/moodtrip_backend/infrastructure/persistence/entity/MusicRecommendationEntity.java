package de.tum.moodtrip_backend.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("music_recommendation")
public class MusicRecommendationEntity {
    @Id
    private Long id;
    
    @Column("conversation_id")
    private Long conversationId;
    
    private String title;
    private String link;
    
    @Column("created_at")
    private LocalDateTime createdAt;

    public MusicRecommendationEntity() {
    }

    public MusicRecommendationEntity(Long id, Long conversationId, String title, String link, LocalDateTime createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.title = title;
        this.link = link;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
