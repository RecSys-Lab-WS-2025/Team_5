package de.tum.moodtrip_backend.adapter_database.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.r2dbc.postgresql.codec.Json;


@Table("route_recommendation")
public class RouteRecommendationEntity {
    @Id
    private Long id;
    
    @Column("conversation_id")
    private Long conversationId;
    
    @Column("route_data")
    private Json routeData;
    
    @Column("created_at")
    private LocalDateTime createdAt;

    public RouteRecommendationEntity() {
    }

    public RouteRecommendationEntity(Long id, Long conversationId, Json routeData, LocalDateTime createdAt) {
        this.id = id;
        this.conversationId = conversationId;
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

    public Json getRouteData() {
        return routeData;
    }

    public void setRouteData(Json routeData) {
        this.routeData = routeData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
