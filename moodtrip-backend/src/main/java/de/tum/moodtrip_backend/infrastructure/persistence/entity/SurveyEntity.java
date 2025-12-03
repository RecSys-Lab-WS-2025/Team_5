package de.tum.moodtrip_backend.infrastructure.persistence.entity;


import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("surveys")
public class SurveyEntity {
    
    @Id
    private Long id;
    
    @Column("user_id")
    private Long userId;
    
    @Column("conversation_id")
    private Long conversationId;
    
    @Column("location")
    private String location;
    
    @Column("range_meters")
    private Integer rangeMeters;
    
    @Column("start_date")
    private LocalDate startDate;
    
    @Column("end_date")
    private LocalDate endDate;
    
    @Column("poi_categories")
    private String poiCategories;
    
    @Column("created_at")
    private LocalDateTime createdAt;

    public SurveyEntity() {
    }

    public SurveyEntity(Long id, Long userId, Long conversationId, String location, Integer rangeMeters,
                        LocalDate startDate, LocalDate endDate, String poiCategories,
                        LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.conversationId = conversationId;
        this.location = location;
        this.rangeMeters = rangeMeters;
        this.startDate = startDate;
        this.endDate = endDate;
        this.poiCategories = poiCategories;
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

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getRangeMeters() {
        return rangeMeters;
    }

    public void setRangeMeters(Integer rangeMeters) {
        this.rangeMeters = rangeMeters;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPoiCategories() {
        return poiCategories;
    }

    public void setPoiCategories(String poiCategories) {
        this.poiCategories = poiCategories;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
