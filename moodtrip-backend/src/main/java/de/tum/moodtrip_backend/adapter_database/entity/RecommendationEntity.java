package de.tum.moodtrip_backend.adapter_database.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("recommendation")
public class RecommendationEntity {
    @Id
    private Long id;
    
    @Column("conversation_id")
    private Long conversationId;
    
    private String type;
    private String title;
    private String description;
    private String link;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("track_id")
    private String trackId;
    
    @Column("route_data")
    private String routeData;

    public RecommendationEntity() {
    }

    public RecommendationEntity(Long id, Long conversationId, String type, String title, 
                                String description, String link, String trackId, String routeData, LocalDateTime createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.link = link;
        this.trackId = trackId;
        this.routeData = routeData;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public String getRouteData() {
        return routeData;
    }

    public void setRouteData(String routeData) {
        this.routeData = routeData;
    }
}
